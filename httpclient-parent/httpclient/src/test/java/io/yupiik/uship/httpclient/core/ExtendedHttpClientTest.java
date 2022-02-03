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
package io.yupiik.uship.httpclient.core;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.yupiik.uship.httpclient.core.listener.RequestListener;
import io.yupiik.uship.httpclient.core.listener.impl.DefaultTimeout;
import io.yupiik.uship.httpclient.core.listener.impl.HARDumperListener;
import io.yupiik.uship.httpclient.core.listener.impl.SetUserAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class ExtendedHttpClientTest {
    @Test
    void send() {
        try (
                final var server = new Server(ex -> {
                    final var body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                    ex.sendResponseHeaders(200, body.length);
                    ex.getResponseBody().write(body);
                    ex.close();
                });
                final var http = newClient()) {
            assertEquals("{\"ok\":true}", http.send(server.GET().build()).body());
            assertEquals(1, http.getRequestCount());
        }
    }

    @Test
    void timeout() {
        try (
                final var server = new Server(ex -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    ex.sendResponseHeaders(200, 0);
                    ex.close();
                });
                final var http = newClient(new DefaultTimeout(Duration.ofMillis(100)))) {
            assertInstanceOf(
                    HttpTimeoutException.class,
                    assertThrows(
                            IllegalStateException.class,
                            () -> http.send(server.GET().build()))
                            .getCause());
        }
    }

    @Test
    void userAgent() {
        try (
                final var server = new Server(ex -> {
                    ex.sendResponseHeaders("test/1.0".equals(ex.getRequestHeaders().getFirst("user-agent")) ? 200 : 500, 0);
                    ex.close();
                });
                final var http = newClient(new SetUserAgent("test/1.0"))) {
            assertEquals(200, http.send(server.GET().build()).statusCode());
        }
    }

    @Test
    void har(@TempDir final Path work) throws IOException {
        final var output = work.resolve("dump.har");
        final String serverBase;
        try (
                final var server = new Server(ex -> {
                    final var body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                    ex.sendResponseHeaders(200, body.length);
                    ex.getResponseBody().write(body);
                    ex.close();
                });
                final var http = newClient(new HARDumperListener(output, Clock.systemUTC(), Logger.getLogger(getClass().getName())))) {
            http.send(server.GET().build());
            serverBase = server.base().toASCIIString();
        }
        assertTrue(Files.exists(output));
        assertEquals(("" +
                        "{\n" +
                        "  \"log\":{\n" +
                        "    \"comment\":\"\",\n" +
                        "    \"entries\":[\n" +
                        "      {\n" +
                        "        \"request\":{\n" +
                        "          \"bodySize\":-1,\n" +
                        "          \"comment\":\"\",\n" +
                        "          \"headerSize\":0,\n" +
                        "          \"headers\":[\n" +
                        "          ],\n" +
                        "          \"httpVersion\":\"HTTP/1.1\",\n" +
                        "          \"method\":\"GET\",\n" +
                        "          \"url\":\"" + serverBase + "\"\n" +
                        "        },\n" +
                        "        \"response\":{\n" +
                        "          \"bodySize\":11,\n" +
                        "          \"comment\":\"\",\n" +
                        "          \"content\":{\n" +
                        "            \"compression\":0,\n" +
                        "            \"size\":11,\n" +
                        "            \"text\":\"{\\\"ok\\\":true}\"\n" +
                        "          },\n" +
                        "          \"headers\":[\n" +
                        "            {\n" +
                        "              \"name\":\"content-length\",\n" +
                        "              \"value\":\"11\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"name\":\"date\",\n" +
                        "              \"value\":\"<date>\"\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"headersSize\":57,\n" +
                        "          \"httpVersion\":\"HTTP/1.1\",\n" +
                        "          \"status\":200,\n" +
                        "          \"statusText\":\"OK\"\n" +
                        "        },\n" +
                        "        \"time\":0\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"version\":\"1.2\"\n" +
                        "  }\n" +
                        "}"),
                Files.readString(output)
                        .replaceAll("\\p{Alpha}+, \\p{Digit}+ \\p{Alpha}+ \\p{Digit}{4} \\p{Digit}{2}:\\p{Digit}{2}:\\p{Digit}{2} \\p{Alpha}+", "<date>"));
    }

    private ExtendedHttpClient newClient(final RequestListener<?>... listeners) {
        return new ExtendedHttpClient(new ExtendedHttpClientConfiguration()
                .setRequestListeners(List.of(listeners)));
    }

    private static class Server implements AutoCloseable {
        private final HttpServer server;

        private Server(final HttpHandler httpHandler) {
            try {
                this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                this.server.createContext("/").setHandler(httpHandler);
                this.server.start();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private URI base() {
            return URI.create("http://localhost:" + server.getAddress().getPort());
        }

        private HttpRequest.Builder GET() {
            return HttpRequest.newBuilder().GET().uri(base());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
