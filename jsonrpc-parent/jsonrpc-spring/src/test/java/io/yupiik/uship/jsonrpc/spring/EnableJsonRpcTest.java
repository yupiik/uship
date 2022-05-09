/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.spring;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = EnableJsonRpcTest.MyApp.class, webEnvironment = RANDOM_PORT)
class EnableJsonRpcTest {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JsonProvider jsonProvider;

    @Test
    void echo() throws IOException, InterruptedException {
        final var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(jsonProvider.createObjectBuilder()
                                .add("jsonrpc", "2.0")
                                .add("method", "reverse")
                                .add("params", jsonProvider.createObjectBuilder()
                                        .add("in", "kiipuy"))
                                .build()
                                .toString(), StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + port + "/jsonrpc"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        assertEquals("{\"jsonrpc\":\"2.0\",\"result\":\"yupiik\"}", response.body());
    }

    @Test
    void request() throws IOException, InterruptedException {
        final var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(jsonProvider.createObjectBuilder()
                                .add("jsonrpc", "2.0")
                                .add("method", "header")
                                .add("params", jsonProvider.createObjectBuilder()
                                        .add("name", "foo"))
                                .build()
                                .toString(), StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + port + "/jsonrpc"))
                        .header("foo", "yes")
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        assertEquals("{\"jsonrpc\":\"2.0\",\"result\":\"yes\"}", response.body());
    }

    @Test
    void openrpcBaseUrl() throws IOException, InterruptedException {
        final var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(jsonProvider.createObjectBuilder()
                                .add("jsonrpc", "2.0")
                                .add("method", "openrpc")
                                .build()
                                .toString(), StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + port + "/jsonrpc"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(":" + port + "/jsonrpc\",\"variables\":{}}"), response.body());
    }

    @EnableJsonRpc
    @SpringBootApplication
    public static class MyApp {
        @Bean
        MyEndpoints endpoints() {
            return new MyEndpoints();
        }
    }

    @JsonRpc
    public static class MyEndpoints {
        @JsonRpcMethod(name = "reverse")
        public String reverse(@JsonRpcParam(value = "in") final String input) {
            return new StringBuilder(input).reverse().toString();
        }

        @JsonRpcMethod(name = "header")
        public String header(@JsonRpcParam(value = "name") final String name,
                             final HttpServletRequest request) {
            return request.getHeader(name);
        }
    }
}
