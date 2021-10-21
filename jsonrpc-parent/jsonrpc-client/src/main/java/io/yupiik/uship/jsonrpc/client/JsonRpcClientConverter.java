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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class JsonRpcClientConverter implements AutoCloseable {
    private final Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomizer;
    private final URI endpoint;
    private final Jsonb jsonb;
    private final JsonBuilderFactory jsonBuilderFactory;
    private boolean closeJsonb;

    public JsonRpcClientConverter(final JsonRpcClientConfiguration clientConfiguration) {
        this.requestCustomizer = ofNullable(clientConfiguration.getRequestCustomizer()).orElseGet(Function::identity);
        this.jsonb = ofNullable(clientConfiguration.getJsonb()).orElseGet(() -> {
            this.closeJsonb = true;
            return JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.skip-cdi", true));
        });
        this.jsonBuilderFactory = ofNullable(clientConfiguration.getJsonBuilderFactory()).orElseGet(() -> Json.createBuilderFactory(Map.of()));
        this.endpoint = URI.create(requireNonNull(clientConfiguration.getEndpoint(), "no endpoint set"));
    }

    public JsonBuilderFactory getJsonBuilderFactory() {
        return jsonBuilderFactory;
    }

    /**
     * @param method the JSON-RPC method to call.
     * @param params the parameters of the call. If null it is ignored else it is serialized. Can be an object, a map or a list.
     * @return the HTTP request representing the JSON-RPC request.
     */
    public HttpRequest toHttpRequest(final String method, final Object params) {
        final JsonObjectBuilder basePayload = toJsonRpcRequest(method, params);
        return requestCustomizer
                .apply(HttpRequest.newBuilder()
                        .uri(endpoint)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(basePayload.build().toString(), UTF_8)))
                .build();
    }

    /**
     * @param bulkRequest the JSON-RPC bulk request (likely built with {@link #toHttpRequest(String, Object)}.
     * @return the HTTP request representing the JSON-RPC request.
     */
    public HttpRequest toHttpRequest(final JsonArray bulkRequest) {
        return requestCustomizer
                .apply(HttpRequest.newBuilder()
                        .uri(endpoint)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bulkRequest.toString(), UTF_8)))
                .build();
    }

    /**
     * @param method the JSON-RPC method to execute.
     * @param params the params of the method.
     * @return the JsonObject representing this JSON-RPC request.
     */
    public JsonObjectBuilder toJsonRpcRequest(final String method, final Object params) {
        final var basePayload = jsonBuilderFactory.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", method);
        if (params != null) {
            basePayload.add("params", JsonValue.class.isInstance(params) ?
                    JsonValue.class.cast(params) :
                    jsonb.fromJson(jsonb.toJson(params), JsonValue.class));
        }
        return basePayload;
    }

    public JsonRpcResponse toJsonRpcResponse(final HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new JsonClientRpcException(new JsonRpcResponse(response, null, jsonb));
        }
        final var result = jsonb.fromJson(response.body(), JsonValue.class);
        switch (result.getValueType()) {
            case OBJECT:
            case ARRAY:
                return new JsonRpcResponse(response, result, jsonb);
            default:
                throw new IllegalArgumentException("Invalid response: " + response.body());
        }
    }

    @Override
    public void close() throws Exception {
        if (closeJsonb) {
            jsonb.close();
        }
    }
}
