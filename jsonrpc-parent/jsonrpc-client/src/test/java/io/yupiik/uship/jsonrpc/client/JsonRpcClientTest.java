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
package io.yupiik.uship.jsonrpc.client;

import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcClientTest {
    private static HttpServer server;
    private static JsonRpcClient client;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(exchange -> {
            final String request;
            try (final var in = exchange.getRequestBody()) {
                request = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            final var bytes = (request.contains("\"simple\"") ?
                    "{\"jsonrpc\":\"2.0\",\"result\":{\"res\":true}}" :
                    request.contains("\"array\"") ?
                            "[{\"jsonrpc\":\"2.0\",\"result\":{\"res\":true}}]" :
                            request.contains("\"error\"") ?
                                    "{\"jsonrpc\":\"2.0\",\"error\":{\"res\":true}}" :
                                    "{}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new JsonRpcClient(new JsonRpcClientConfiguration("http://localhost:" + server.getAddress().getPort() + "/jsonrpc"));
    }

    @AfterAll
    static void stopServer() {
        client.close();
        server.stop(0);
    }

    @Test
    void simple() throws IOException, InterruptedException {
        final var response = client.execute("simple", Map.of("foo", "bar"));
        assertTrue(response.isSingle());
        assertFalse(response.isArray());

        final var single = response.asSingle();
        assertTrue(single.hasResult());
        assertFalse(single.isError());
        assertEquals(Map.of("res", true), single.as(Map.class));
    }

    @Test
    void array() throws IOException, InterruptedException {
        final var response = client.execute(Json.createArrayBuilder()
                .add(client.getProtocol().toJsonRpcRequest("array", Map.of("foo", "bar")))
                .build());
        assertFalse(response.isSingle());
        assertTrue(response.isArray());

        final var array = response.asArray();
        final var all = array.list();
        assertEquals(1, all.size());
        assertFalse(array.hasFailure());

        final var single = all.iterator().next();
        assertTrue(single.hasResult());
        assertFalse(single.isError());
        assertEquals(Map.of("res", true), single.as(Map.class));
    }

    @Test
    void error() throws IOException, InterruptedException {
        final var response = client.execute("error", Map.of("foo", "bar"));
        assertTrue(response.isSingle());
        assertFalse(response.isArray());
        final var single = response.asSingle();
        assertFalse(single.hasResult());
        assertTrue(single.isError());
        assertEquals(Map.of("res", true), single.errorAs(Map.class));
    }
}
