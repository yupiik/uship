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
package io.yupiik.uship.jsonrpc.core.api.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.bind.Jsonb;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Enables to extract all methods from a JSON-RPC request.
 * It is very useful to implement ACL in a service.
 */
@ApplicationScoped
public class JsonRpcExtractor {
    @Inject
    private Jsonb jsonb;

    public List<String> findJsonRpcMethods(final JsonStructure request) {
        return findJsonRpcMethods(request, obj -> Stream.of(obj.asJsonObject().getString("method")));
    }

    public List<String> findJsonRpcMethods(final JsonStructure request, final Function<JsonObject, Stream<String>> recursiveExtractor) {
        switch (request.getValueType()) {
            case OBJECT:
                return recursiveExtractor.apply(request.asJsonObject()).collect(toList());
            case ARRAY:
                return request.asJsonArray().stream()
                        .flatMap(it -> findJsonRpcMethods(it.asJsonObject(), recursiveExtractor).stream())
                        .collect(toList());
            default:
                throw new IllegalArgumentException("Invalid parameter: " + request.getValueType());
        }
    }
}
