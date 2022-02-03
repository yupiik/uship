/*
 * Copyright (c) 2021, 2022 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.uship.tracing.server;

import io.yupiik.uship.tracing.collector.AccumulatingSpanCollector;
import io.yupiik.uship.tracing.span.Span;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class TracingValve extends ValveBase {
    private final ServerTracingConfiguration configuration;
    private final AccumulatingSpanCollector collector;
    private final Supplier<Object> idGenerator;
    private final Clock clock;

    public TracingValve(final ServerTracingConfiguration configuration,
                        final AccumulatingSpanCollector collector,
                        final Supplier<Object> idGenerator,
                        final Clock clock) {
        super(true);
        this.configuration = requireNonNull(configuration, "configuration must be not null");
        this.collector = requireNonNull(collector, "collector must be not null");
        this.idGenerator = requireNonNull(idGenerator, "idGenerator must be not null");
        this.clock = requireNonNull(clock, "clock must be not null");
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        try {
            super.stopInternal();
        } finally {
            if (AccumulatingSpanCollector.class.isInstance(collector)) {
                AccumulatingSpanCollector.class.cast(collector).close();
            }
        }
    }

    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        final var tags = new HashMap<>(configuration.getTags());
        tags.putIfAbsent("http.url", request.getRequestURI());
        tags.putIfAbsent("http.method", request.getMethod());

        final var traceTrace = request.getHeader(configuration.getTraceHeader());
        final var spanTrace = request.getHeader(configuration.getSpanHeader());

        final var span = newSpan();
        span.setKind("SERVER");
        span.setId(idGenerator.get());
        span.setName(configuration.getOperation());
        span.setTags(tags);

        if (spanTrace != null) {
            span.setParentId(spanTrace);
        }
        if (traceTrace != null) {
            span.setTraceId(traceTrace);
        } else {
            span.setTraceId(idGenerator.get());
        }

        final var localEndpoint = new Span.Endpoint();
        final var remoteAddr = request.getRemoteAddr();
        if (remoteAddr.contains("::")) {
            localEndpoint.setIpv6(remoteAddr);
        } else {
            localEndpoint.setIpv4(remoteAddr);
        }
        localEndpoint.setServiceName(configuration.getServiceName());
        localEndpoint.setPort(request.getServerPort());
        span.setLocalEndpoint(localEndpoint);

        final var start = clock.instant();
        span.setTimestamp(TimeUnit.MILLISECONDS.toMicros(start.toEpochMilli()));

        afterSpanInit(span);
        request.setAttribute(Span.class.getName(), span);
        try {
            getNext().invoke(request, response);
        } finally {
            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new AsyncListener() {
                    private void status(final AsyncEvent event) {
                        finish(HttpServletResponse.class.cast(event.getSuppliedResponse()), span, start);
                        collectSpan(span);
                    }

                    @Override
                    public void onComplete(final AsyncEvent event) {
                        status(event);
                    }

                    @Override
                    public void onTimeout(final AsyncEvent event) {
                        tags.putIfAbsent("http.error", "timeout");
                        status(event);
                    }

                    @Override
                    public void onError(final AsyncEvent event) {
                        tags.putIfAbsent("http.error", event.getThrowable() == null ?
                                "unknown" :
                                (event.getThrowable().getMessage() == null ? event.getThrowable().getClass().getName() : event.getThrowable().getMessage()));
                        status(event);
                    }

                    @Override
                    public void onStartAsync(final AsyncEvent event) {
                        request.getAsyncContext().addListener(this);
                    }
                });
            } else {
                finish(response, span, start);
                collectSpan(span);
            }
        }
    }

    protected void finish(final HttpServletResponse response, final Span span, final Instant start) {
        final var end = clock.instant();
        span.getTags().putIfAbsent("http.status", response.getStatus());
        span.setDuration(TimeUnit.MILLISECONDS.toMicros(end.minusMillis(start.toEpochMilli()).toEpochMilli()));
    }

    protected void collectSpan(final Span span) {
        collector.accept(span);
    }

    protected void afterSpanInit(final Span span) {
        // no-op
    }

    protected Span newSpan() {
        return new Span();
    }
}
