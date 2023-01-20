/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.tracing.client;

import io.yupiik.uship.httpclient.core.listener.RequestListener;
import io.yupiik.uship.httpclient.core.request.UnlockedHttpRequest;
import io.yupiik.uship.tracing.collector.AccumulatingSpanCollector;
import io.yupiik.uship.tracing.span.Span;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class TracingListener implements RequestListener<TracingListener.State> {
    private final ClientTracingConfiguration configuration;
    private final AccumulatingSpanCollector collector;
    private final Supplier<Object> idGenerator;
    private final Supplier<Span> contextAttributeEvaluator;
    private final Clock clock;
    private final Map<String, String> ips = new ConcurrentHashMap<>();

    public TracingListener(final ClientTracingConfiguration configuration,
                           final AccumulatingSpanCollector collector,
                           final Supplier<Object> idGenerator,
                           final Supplier<Span> contextAttributeEvaluator,
                           final Clock clock) {
        this.configuration = requireNonNull(configuration, "configuration can't be null");
        this.collector = requireNonNull(collector, "collector can't be null");
        this.idGenerator = requireNonNull(idGenerator, "contextAttributeEvaluator can't be null");
        this.contextAttributeEvaluator = requireNonNull(contextAttributeEvaluator, "contextAttributeEvaluator can't be null");
        this.clock = requireNonNull(clock, "clock can't be null");
    }

    @Override
    public RequestListener.State<State> before(final long count, final HttpRequest request) {
        final var tags = new HashMap<>(configuration.getTags());
        tags.putIfAbsent("http.url", request.uri().toASCIIString());
        tags.putIfAbsent("http.method", request.method());
        tags.putIfAbsent("peer.hostname", request.uri().getHost());
        tags.putIfAbsent("peer.port", request.uri().getPort());

        final var endpoint = new Span.Endpoint();
        endpoint.setServiceName(configuration.getServiceName());
        endpoint.setPort(request.uri().getPort());
        final var ip = ipOf(request.uri().getHost());
        if (ip.contains("::")) {
            endpoint.setIpv6(ip);
        } else {
            endpoint.setIpv4(ip);
        }

        final var span = newSpan();
        span.setKind("CLIENT");
        span.setId(idGenerator.get());
        span.setName(configuration.getOperation());
        span.setTags(tags);
        span.setRemoteEndpoint(endpoint);

        final var parent = contextAttributeEvaluator.get();
        if (parent != null) {
            span.setParentId(parent.getId());
            span.setTraceId(parent.getTraceId());
        } else {
            span.setTraceId(idGenerator.get());
        }

        final var start = clock.instant();
        span.setTimestamp(TimeUnit.MILLISECONDS.toMicros(start.toEpochMilli()));
        afterSpanInit(span);

        return new RequestListener.State<>(new UnlockedHttpRequest(
                request.bodyPublisher(), request.method(), request.timeout(), request.expectContinue(), request.uri(), request.version(),
                HttpHeaders.of(customizeHeaders(request, span), (a, b) -> true)),
                new State(start, span));
    }

    private String ipOf(final String host) {
        return ips.computeIfAbsent(host, h -> {
            try {
                return InetAddress.getByName(host).getHostAddress();
            } catch (final UnknownHostException uhe) {
                return host;
            }
        });
    }

    @Override
    public void after(final State state, final HttpRequest request, final Throwable error, final HttpResponse<?> response) {
        if (response != null) {
            state.span.getTags().putIfAbsent("http.status", response.statusCode());
        }
        if (error != null) {
            state.span.getTags().putIfAbsent("http.error", error.getMessage() == null ? error.getClass().getName() : error.getMessage());
        }
        final var end = clock.instant();
        state.span.setDuration(TimeUnit.MILLISECONDS.toMicros(end.minusMillis(state.start.toEpochMilli()).toEpochMilli()));
        collectSpan(state.span);
    }

    protected HashMap<String, List<String>> customizeHeaders(final HttpRequest request, final Span span) {
        final var headers = new HashMap<>(request.headers().map());
        headers.put(configuration.getTraceHeader(), List.of(String.valueOf(span.getTraceId())));
        headers.put(configuration.getSpanHeader(), List.of(String.valueOf(span.getId())));
        headers.put(configuration.getParentHeader(), List.of(String.valueOf(span.getParentId())));
        return headers;
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

    static class State {
        private final Instant start;
        private final Span span;

        private State(final Instant start, final Span span) {
            this.start = start;
            this.span = span;
        }
    }
}
