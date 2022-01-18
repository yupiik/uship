/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
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

import io.yupiik.uship.tracing.collector.AccumulatingSpanCollector;
import io.yupiik.uship.tracing.id.IdGenerator;
import io.yupiik.uship.tracing.span.Span;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static java.time.Clock.systemUTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracingListenerTest {
    @Test
    void trace() {
        final var spans = new ArrayList<Span>();
        try (final var accumulator = new AccumulatingSpanCollector().setOnFlush(spans::addAll)) {
            final var listener = new TracingListener(
                    new ClientTracingConfiguration(),
                    accumulator,
                    new IdGenerator(IdGenerator.Type.COUNTER),
                    () -> null,
                    systemUTC());
            assertTrue(spans.isEmpty());
            final var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("http://localhost/foo?bar=dummy"))
                    .build();
            final var state = listener.before(-1, request);
            listener.after(state.state(), null, null, response(request));
        }

        assertEquals(1, spans.size());

        final var iterator = spans.iterator();

        final var span1 = iterator.next();
        assertNull(span1.getParentId());
        assertEquals(2L, span1.getTraceId());
        assertEquals(1L, span1.getId());
        assertEquals("httpclient", span1.getName());
        assertEquals("CLIENT", span1.getKind());
        assertEquals(Map.of(
                "component", "yupiik-httpclient",
                "http.method", "GET",
                "http.url", "http://localhost/foo?bar=dummy",
                "http.status", 123,
                "peer.hostname", "localhost",
                "peer.port", -1
        ), span1.getTags());
        assertNotNull(span1.getTimestamp());
        assertNotNull(span1.getDuration());
    }

    @Test
    void traceWithParent() {
        final var spans = new ArrayList<Span>();
        try (final var accumulator = new AccumulatingSpanCollector().setOnFlush(spans::addAll)) {
            final var listener = new TracingListener(
                    new ClientTracingConfiguration(),
                    accumulator,
                    new IdGenerator(IdGenerator.Type.COUNTER),
                    () -> {
                        final var span = new Span();
                        span.setId(-1);
                        span.setTraceId(-2);
                        span.setParentId(-3);
                        return span;
                    },
                    systemUTC());
            assertTrue(spans.isEmpty());
            final var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("http://localhost/foo?bar=dummy"))
                    .build();
            final var state = listener.before(-1, request);
            listener.after(state.state(), null, null, response(request));
        }

        assertEquals(1, spans.size());

        final var iterator = spans.iterator();

        final var span1 = iterator.next();
        assertEquals(-1, span1.getParentId());
        assertEquals(-2, span1.getTraceId());
        assertEquals(1L, span1.getId());
        assertEquals("httpclient", span1.getName());
        assertEquals("CLIENT", span1.getKind());
        assertEquals(Map.of(
                "component", "yupiik-httpclient",
                "http.method", "GET",
                "http.url", "http://localhost/foo?bar=dummy",
                "http.status", 123,
                "peer.hostname", "localhost",
                "peer.port", -1
        ), span1.getTags());
        assertNotNull(span1.getTimestamp());
        assertNotNull(span1.getDuration());
        assertNotNull(span1.getRemoteEndpoint());
    }

    private HttpResponse<String> response(final HttpRequest request) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 123;
            }

            @Override
            public HttpRequest request() {
                return request;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return "";
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return request.uri();
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
