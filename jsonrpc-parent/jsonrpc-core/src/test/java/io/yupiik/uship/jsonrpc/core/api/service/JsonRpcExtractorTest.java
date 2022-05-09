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
package io.yupiik.uship.jsonrpc.core.api.service;

import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Cdi(classes = JsonRpcExtractor.class)
class JsonRpcExtractorTest {
    @Inject
    private JsonRpcExtractor extractor;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Test
    void extractObject() {
        assertEquals(
                List.of("m1"),
                extractor.findJsonRpcMethods(jsonBuilderFactory.createObjectBuilder()
                        .add("method", "m1")
                        .build()));
    }

    @Test
    void extractBulk() {
        assertEquals(
                List.of("m1", "m2"),
                extractor.findJsonRpcMethods(jsonBuilderFactory.createArrayBuilder()
                        .add(jsonBuilderFactory.createObjectBuilder()
                                .add("method", "m1"))
                        .add(jsonBuilderFactory.createObjectBuilder()
                                .add("method", "m2"))
                        .build()));
    }

    @Test
    void extractCustom() {
        assertEquals(
                List.of("m1", "sub", "m2", "sub", "m3"),
                extractor.findJsonRpcMethods(
                        jsonBuilderFactory.createArrayBuilder()
                                .add(jsonBuilderFactory.createObjectBuilder()
                                        .add("method", "m1"))
                                .add(jsonBuilderFactory.createObjectBuilder()
                                        .add("method", "sub")
                                        .add("params", jsonBuilderFactory.createObjectBuilder()
                                                .add("bulk", jsonBuilderFactory.createArrayBuilder()
                                                        .add(jsonBuilderFactory.createObjectBuilder()
                                                                .add("method", "m2"))
                                                        .add(jsonBuilderFactory.createObjectBuilder()
                                                                .add("method", "sub")
                                                                .add("params", jsonBuilderFactory.createObjectBuilder()
                                                                        .add("bulk", jsonBuilderFactory.createArrayBuilder()
                                                                                .add(jsonBuilderFactory.createObjectBuilder()
                                                                                        .add("method", "m3"))))))))
                                .build(),
                        this::extractCustom));
    }

    private Stream<String> extractCustom(final JsonObject e) {
        final var method = e.getString("method");
        return "sub".equals(method) ?
                Stream.concat(
                        Stream.of("sub"),
                        e.getJsonObject("params").getJsonArray("bulk").stream()
                                .map(JsonValue::asJsonObject)
                                .map(it -> extractor.findJsonRpcMethods(it, this::extractCustom))
                                .flatMap(Collection::stream)) :
                Stream.of(method);
    }
}
