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
package io.yupiik.uship.jsonrpc.core.impl;

import io.yupiik.uship.backbone.johnzon.Object2JsonSerializer;
import io.yupiik.uship.jsonrpc.core.lang.Tuple2;
import io.yupiik.uship.jsonrpc.core.protocol.JsonRpcException;
import io.yupiik.uship.jsonrpc.core.protocol.Response;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class SimpleJsonRpcHandler {
    public interface Constants {
        String REQUEST_METHOD_ATTRIBUTE = "yupiik.jsonrpc.method";
    }

    private final Logger logger = Logger.getLogger(getClass().getName());

    private Jsonb jsonb;
    private SimpleJsonRpcMethodRegistry registry;
    private Object2JsonSerializer toJsonValue;

    protected void setJsonb(final Jsonb jsonb) {
        this.jsonb = jsonb;
        this.toJsonValue = new Object2JsonSerializer(jsonb);
    }

    protected void setRegistry(final SimpleJsonRpcMethodRegistry registry) {
        this.registry = registry;
    }

    public JsonStructure readRequest(final Reader reader) throws IOException {
        try (final var in = reader) {
            return jsonb.fromJson(in, JsonStructure.class);
        }
    }

    public CompletionStage<Response> handleRequest(final JsonObject request, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        return doValidate(request)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> doHandle(request, httpRequest, httpResponse));
    }

    public CompletableFuture<Response> doHandle(final JsonObject request, final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) {
        final var method = request.getString("method");
        final var fn = registry.getHandlers().get(method).executor();
        final var id = request.get("id");
        final var params = ofNullable(request.get("params")).map(JsonStructure.class::cast).orElse(null);

        if (servletRequest != null) {
            appendJsonRpcMethod(servletRequest, method);
        }

        try {
            return fn.apply(params, new Tuple2<>(servletRequest, servletResponse)).handle((result, error) -> {
                if (error != null) {
                    return toErrorResponse(
                            id,
                            CompletionException.class.isInstance(error) && error.getCause() != null ?
                                    error.getCause() : error,
                            request);
                }
                return new Response("2.0", id, result, null);
            }).toCompletableFuture();
        } catch (final RuntimeException re) {
            return completedFuture(toErrorResponse(id, re, request));
        }
    }

    private void appendJsonRpcMethod(final HttpServletRequest httpRequest, final String method) {
        final String existing = String.class.cast(httpRequest.getAttribute(Constants.REQUEST_METHOD_ATTRIBUTE));
        if (existing == null) {
            httpRequest.setAttribute(Constants.REQUEST_METHOD_ATTRIBUTE, method);
        } else {
            httpRequest.setAttribute(Constants.REQUEST_METHOD_ATTRIBUTE, existing + "," + method);
        }
    }

    public Response toErrorResponse(final JsonValue id, final Throwable re, final JsonStructure request) {
        final Response.ErrorResponse errorResponse;

        if (JsonRpcException.class.isInstance(re)) {
            final var jsonRpcException = JsonRpcException.class.cast(re);
            final var data = jsonRpcException.getData();
            errorResponse = new Response.ErrorResponse(
                    jsonRpcException.getCode(), re.getMessage(),
                    data == null || JsonValue.class.isInstance(data) ?
                            JsonValue.class.cast(data) :
                            toJsonValue.apply(data));
        } else {
            errorResponse = new Response.ErrorResponse(-2, re.getMessage(), null);
        }

        if (logger.isLoggable(Level.FINEST)) { // log the stack too, else just the origin method
            logger.log(Level.SEVERE, "An error occured calling /jsonrpc '" + request + "'", re);
        } else {
            logger.log(Level.SEVERE, "An error occured calling /jsonrpc, " +
                    (request.getValueType() == JsonValue.ValueType.OBJECT ? "method=" + request.asJsonObject().get("method") : request), re);
        }

        return new Response("2.0", id, null, errorResponse);
    }

    public Optional<Response> doValidate(final JsonObject request) {
        final var pair = ensurePresent(request, "jsonrpc", -32600);
        if (pair.second() != null) {
            return of(pair.second());
        }
        if (!"2.0".equals(pair.first())) {
            return of(createResponse(request, -32600, "invalid jsonrpc version"));
        }
        final var method = ensurePresent(request, "method", -32601);
        if (method.second() != null) {
            return of(method.second());
        }
        if (!registry.getHandlers().containsKey(method.first())) {
            return of(createResponse(request, -32601, "Unknown method (" + method.first() + ")"));
        }
        return empty();
    }

    private Tuple2<String, Response> ensurePresent(final JsonObject request, final String key, final int code) {
        final var methodJson = request.getJsonString(key);
        if (methodJson == null) {
            return new Tuple2<>(null, createResponse(request, code, "Missing " + key));
        }

        final String method = methodJson.getString();
        if (method.isEmpty()) {
            return new Tuple2<>(null, createResponse(request, code, "Empty " + key));
        }

        return new Tuple2<>(method, null);
    }

    public Response createResponse(final JsonObject request, final int code, final String message) {
        final var id = request == null ? null : request.get("id");
        return new Response("2.0", id, null, new Response.ErrorResponse(code, message, null));
    }

    public CompletionStage<?> execute(final JsonStructure request, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        switch (request.getValueType()) {
            case OBJECT: // single request
                return handleRequest(request.asJsonObject(), httpRequest, httpResponse);
            case ARRAY: // batch
                final var requests = request.asJsonArray();
                if (requests.size() > 50) {
                    return completedFuture(toErrorResponse(null, new JsonRpcException(10_100, "Too much request at once, limit it to 50 max please.", null), request));
                }
                final CompletableFuture<?>[] futures = requests.stream()
                        .map(it -> it.getValueType() == JsonValue.ValueType.OBJECT ?
                                handleRequest(it.asJsonObject(), httpRequest, httpResponse) :
                                completedFuture(createResponse(it.asJsonObject(), -32600, "Batch requests must be JSON objects")))
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures)
                        .thenApply(ignored -> Stream.of(futures)
                                .map(f -> f.getNow(null))
                                .toArray(Response[]::new));
            default:
                return completedFuture(createResponse(null, -32600, "Unknown request type: " + request.getValueType()));
        }
    }
}
