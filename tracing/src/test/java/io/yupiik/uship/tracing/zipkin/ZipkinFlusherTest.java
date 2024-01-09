/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.tracing.zipkin;

import com.sun.net.httpserver.HttpServer;
import io.yupiik.uship.tracing.span.Span;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZipkinFlusherTest {
    @Test
    void post() throws Exception {
        final var payloads = new ArrayList<String>();
        final var server = HttpServer.create(new InetSocketAddress(0), 64);
        server.createContext("/").setHandler(ex -> {
            try {
                try (final var reader = ex.getRequestBody()) {
                    payloads.add(new String(reader.readAllBytes(), StandardCharsets.UTF_8));
                }
                ex.sendResponseHeaders(200, 0);
            } finally {
                ex.close();
            }
        });

        final var endpoint = new Span.Endpoint();
        endpoint.setIpv4("1.2.3.4");
        endpoint.setPort(6543);

        final var span = new Span();
        span.setTimestamp(1234L);
        span.setTraceId(1);
        span.setId("2");
        span.setParentId("zios");
        span.setRemoteEndpoint(endpoint);
        span.setTags(Map.of("foo", "bar"));

        server.start();

        final var configuration = new ZipkinFlusherConfiguration();
        configuration.setUrls(List.of("http://localhost:" + server.getAddress().getPort() + "/zipkin"));

        try (final var jsonb = JsonbBuilder.create()) {
            new ZipkinFlusher(jsonb, HttpClient.newHttpClient(), configuration)
                    .accept(List.of(span));
        } finally {
            server.stop(0);
        }

        assertEquals(1, payloads.size());
        assertEquals("[{" +
                "\"traceId\":1,\"parentId\":\"zios\",\"id\":\"2\",\"timestamp\":1234," +
                "\"remoteEndpoint\":{\"ipv4\":\"1.2.3.4\",\"port\":6543}," +
                "\"tags\":{\"foo\":\"bar\"}}]", payloads.get(0));
    }
}
