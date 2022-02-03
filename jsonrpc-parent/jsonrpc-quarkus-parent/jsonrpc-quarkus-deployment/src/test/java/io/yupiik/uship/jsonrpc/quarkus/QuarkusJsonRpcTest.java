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
package io.yupiik.uship.jsonrpc.quarkus;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class QuarkusJsonRpcTest {
    @TestHTTPResource
    URL url;

    @Inject
    JsonProvider jsonProvider;

    @Test
    void run() throws URISyntaxException, IOException, InterruptedException {
        final var res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(
                                jsonProvider.createObjectBuilder()
                                        .add("jsonrpc", "2.0")
                                        .add("method", "hi")
                                        .add("params", jsonProvider.createObjectBuilder()
                                                .add("name", "test"))
                                        .build()
                                        .toString()))
                        .uri(url.toURI().resolve("/jsonrpc"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(UTF_8));
        assertEquals(200, res.statusCode(), res::body);
        assertEquals("{\"jsonrpc\":\"2.0\",\"result\":\"Hi test\"}", res.body());
    }
}
