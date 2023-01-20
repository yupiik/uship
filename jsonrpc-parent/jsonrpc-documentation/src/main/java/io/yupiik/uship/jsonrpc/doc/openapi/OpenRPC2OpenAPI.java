/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.doc.openapi;

import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class OpenRPC2OpenAPI implements Function<OpenRPC2OpenAPI.Configuration, JsonObject> {
    private final JsonBuilderFactory jsonBuilderFactory;
    private final Jsonb jsonb;

    public OpenRPC2OpenAPI(final JsonBuilderFactory jsonBuilderFactory, final Jsonb jsonb) {
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.jsonb = jsonb;
    }

    public JsonObject apply(final Configuration configuration) {
        final var openRpc = configuration.getOpenRPCLoader().apply(jsonb);
        final var tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        return jsonBuilderFactory.createObjectBuilder()
                .add("openapi", configuration.getOpenapiVersion())
                .add("servers", createServers(configuration, openRpc))
                .add("info", createInfo(configuration))
                .add("paths", createPaths(configuration, openRpc, tags))
                .add("tags", createTags(tags))
                .add("components", createComponents(openRpc))
                .build();
    }

    protected JsonObjectBuilder createPaths(final Configuration configuration,
                                            final OpenRPC openRpc,
                                            final Set<String> tags) {
        final var voidSchema = jsonBuilderFactory.createObjectBuilder().add("type", "object").build();
        return openRpc.getMethods().stream()
                .collect(Collector.of(
                        jsonBuilderFactory::createObjectBuilder,
                        (a, i) -> {
                            try {
                                addMethod(configuration, tags, voidSchema, a, i);
                            } catch (final RuntimeException re) {
                                throw new IllegalStateException(re.getMessage() + " for: " + i, re);
                            }
                        },
                        JsonObjectBuilder::addAll));
    }

    protected void addMethod(final Configuration configuration,
                             final Set<String> tags,
                             final JsonObject voidSchema,
                             final JsonObjectBuilder objectBuilder,
                             final OpenRPC.RpcMethod method) {
        final var tagValues = configuration.getTagProvider() != null ?
                configuration.getTagProvider().apply(method.getName()) : JsonValue.EMPTY_JSON_ARRAY;
        tagValues.stream().map(JsonString.class::cast).map(JsonString::getString).forEach(tags::add);

        final var base = jsonBuilderFactory.createObjectBuilder()
                .add("operationId", method.getName())
                .add("summary", method.getDescription())
                .add("requestBody", createRequestBody(configuration, method))
                .add("responses", createResponses(configuration, voidSchema, method));
        if (!tagValues.isEmpty()) {
            base.add("tags", tagValues);
        }

        objectBuilder.add(toMethodPath(method), jsonBuilderFactory.createObjectBuilder().add("post", base));
    }

    protected String toMethodPath(final OpenRPC.RpcMethod method) {
        return '/' + method.getName();
    }

    protected JsonObjectBuilder createRequestBody(final Configuration configuration,
                                                  final OpenRPC.RpcMethod method) {
        final var params = toJsonSchema(method.getParams()).build();
        return jsonBuilderFactory.createObjectBuilder()
                .add("content", jsonBuilderFactory.createObjectBuilder()
                        .add("application/json", jsonBuilderFactory.createObjectBuilder()
                                .add("schema", configuration.isWrapPayload() ? wrapParams(params, method) : params)));
    }

    protected JsonObject wrapParams(final JsonObject params, final OpenRPC.RpcMethod method) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", jsonBuilderFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("default", "2.0")
                                .add("description", "JSON-RPC version, should always be '2.0'."))
                        .add("method", jsonBuilderFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("default", method.getName())
                                .add("description", "The JSON-RPC method name, should always be '" + method.getName() + "'"))
                        .add("params", params))
                .add("required", jsonBuilderFactory.createArrayBuilder()
                        .add("jsonrpc")
                        .add("method"))
                .build();
    }

    protected JsonObjectBuilder createResponses(final Configuration configuration,
                                                final JsonObject voidSchema,
                                                final OpenRPC.RpcMethod method) {
        final var resultSchema = method.getResult() == null ?
                voidSchema :
                stripId(toJsonObject(method.getResult().getSchema()));
        final var ok = create200Response(configuration, resultSchema);
        final var base = jsonBuilderFactory.createObjectBuilder()
                .add("200", ok);

        final var errors = method.getErrors();
        if (errors == null || errors.isEmpty()) {
            return base;
        }

        errors.forEach(it -> base.add("x-jsonrpc-code=" + it.getCode(), createErrorResponse(configuration, it)));
        return base;
    }

    protected JsonObjectBuilder createErrorResponse(final Configuration configuration, final OpenRPC.ErrorValue error) {
        final var schema = error.getData() == null ?
                jsonBuilderFactory.createObjectBuilder().add("type", "object").build() :
                stripId(toJsonObject(error.getData()));
        final var errorCode = "Error code=" + error.getCode();
        return jsonBuilderFactory.createObjectBuilder()
                .add("description", ofNullable(error.getMessage()).map(it -> it + " (" + errorCode + ")").orElse(errorCode))
                .add("content", jsonBuilderFactory.createObjectBuilder()
                        .add("application/json", jsonBuilderFactory.createObjectBuilder()
                                .add("schema", configuration.isWrapPayload() ? wrapError(error.getCode(), error.getMessage(), schema) : schema)));
    }

    protected JsonObjectBuilder create200Response(final Configuration configuration, final JsonObject resultSchema) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("description", "OK")
                .add("content", jsonBuilderFactory.createObjectBuilder()
                        .add("application/json", jsonBuilderFactory.createObjectBuilder()
                                .add("schema", configuration.isWrapPayload() ? wrapResult(resultSchema) : resultSchema)));
    }

    protected JsonObject wrapError(int code, String message, final JsonObject schema) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", jsonBuilderFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("default", "2.0")
                                .add("description", "JSON-RPC version, should always be '2.0'."))
                        .add("error", jsonBuilderFactory.createObjectBuilder()
                            .add("type", "object")
                            .add("properties", jsonBuilderFactory.createObjectBuilder()
                                .add("code", jsonBuilderFactory.createObjectBuilder()
                                    .add("type", "integer")
                                    .add("default", code)
                                    .add("description", "A Number that indicates the error type that occurred. This MUST be an integer."))
                                .add("message", jsonBuilderFactory.createObjectBuilder()
                                    .add("type", "string")
                                    .add("default", message)
                                    .add("description", "A String providing a short description of the error. The message SHOULD be limited to a concise single sentence."))
                                .add("data", schema))
                            .add("required", jsonBuilderFactory.createArrayBuilder()
                                .add("code")
                                .add("message"))
                        ))
                .add("required", jsonBuilderFactory.createArrayBuilder()
                        .add("jsonrpc")
                        .add("error"))
                .build();
    }

    protected JsonObject wrapResult(final JsonObject params) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", jsonBuilderFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("default", "2.0")
                                .add("description", "JSON-RPC version, should always be '2.0'."))
                        .add("result", params))
                .add("required", jsonBuilderFactory.createArrayBuilder()
                        .add("jsonrpc")
                        .add("result"))
                .build();
    }

    protected JsonArrayBuilder createTags(final Set<String> tags) {
        return tags.stream()
                .collect(Collector.of(
                        jsonBuilderFactory::createArrayBuilder,
                        (b, n) -> b.add(jsonBuilderFactory.createObjectBuilder()
                                .add("name", n)
                                .add("description", n)),
                        JsonArrayBuilder::addAll));
    }

    protected JsonObjectBuilder createComponents(final OpenRPC openRpc) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("schemas", createSchemas(openRpc));
    }

    protected JsonObjectBuilder createSchemas(final OpenRPC openRpc) {
        return jsonBuilderFactory.createObjectBuilder(openRpc.getComponents().getSchemas().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> stripId(toJsonObject(e.getValue())))));
    }

    protected JsonArray createServers(final Configuration configuration, final OpenRPC openRpc) {
        return configuration.getServers() == null ?
                openRpc.getServers().stream()
                        .map(s -> jsonBuilderFactory.createObjectBuilder()
                                .add("url", s.getUrl())
                                .add("description", ofNullable(s.getSummary()).orElse("")))
                        .collect(Collector.of(
                                jsonBuilderFactory::createArrayBuilder,
                                JsonArrayBuilder::add,
                                JsonArrayBuilder::addAll,
                                JsonArrayBuilder::build)) :
                configuration.getServers();
    }

    protected JsonObjectBuilder createInfo(Configuration configuration) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("title", configuration.getTitle())
                .add("description", configuration.getDescription())
                .add("version", configuration.getVersion());
    }

    protected JsonObject toJsonObject(final Object data) {
        return jsonb.fromJson(jsonb.toJson(data), JsonObject.class);
    }

    protected JsonObject stripId(final JsonObject jsonObject) {
        return jsonBuilderFactory.createObjectBuilder(jsonObject.entrySet().stream()
                        .filter(it -> !"$id".equals(it.getKey()))
                        .collect(toMap(Map.Entry::getKey, entry -> {
                            switch (entry.getValue().getValueType()) {
                                case ARRAY:
                                    return jsonBuilderFactory
                                            .createArrayBuilder(entry.getValue().asJsonArray().stream()
                                                    .map(i -> i.getValueType() == JsonValue.ValueType.OBJECT ?
                                                            stripId(i.asJsonObject()) : i)
                                                    .collect(toList()))
                                            .build();
                                case OBJECT:
                                    return stripId(entry.getValue().asJsonObject());
                                default:
                                    return entry.getValue();
                            }
                        })))
                .build();
    }

    protected JsonObjectBuilder toJsonSchema(final Collection<OpenRPC.Value> params) {
        final var required = new ArrayList<String>();
        final var base = jsonBuilderFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", params.stream()
                        .peek(it -> {
                            if (it.getRequired() != null && it.getRequired()) {
                                required.add(it.getName());
                            }
                        })
                        .collect(Collector.of(
                                jsonBuilderFactory::createObjectBuilder,
                                (a, i) -> a.add(i.getName(), stripId(toJsonObject(i.getSchema()))),
                                JsonObjectBuilder::addAll)));
        if (required.isEmpty()) {
            return base;
        }
        return base
                .add("required", required.stream()
                        .collect(Collector.of(
                                jsonBuilderFactory::createArrayBuilder,
                                JsonArrayBuilder::add,
                                JsonArrayBuilder::addAll)));
    }

    public static class Configuration {
        private Function<Jsonb, OpenRPC> openRPCLoader;
        private Function<String, JsonArray> tagProvider;
        private String openapiVersion = "3.0.3"; // swagger-ui does not support 3.1 yet
        private String title;
        private String description;
        private String version;
        private JsonArray servers;
        private boolean wrapPayload = true;

        public Function<String, JsonArray> getTagProvider() {
            return tagProvider;
        }

        public Configuration setTagProvider(final Function<String, JsonArray> tagProvider) {
            this.tagProvider = tagProvider;
            return this;
        }

        public boolean isWrapPayload() {
            return wrapPayload;
        }

        /**
         * Enable to toggle off the wrapping of payload requests in swagger-ui.
         * If set to {@code false}, you need to set a {@code requestInterceptor} in Swagger UI configuration:
         *
         * <pre>
         *     <code>
         *  requestInterceptor: function (request) {
         *    if (request.loadSpec) {
         *      return request;
         *    }
         *    var method = request.url.substring(request.url.lastIndexOf('/') + 1);
         *    return Object.assign(request, {
         *      url: spec.servers.filter(function (server) { return request.url.indexOf(server.url) === 0; })[0].url,
         *      body: JSON.stringify({ jsonrpc: '2.0', method: method, params: JSON.parse(request.body) }, undefined, 2)
         *    });
         *  }
         *     </code>
         * </pre>
         *
         * @param wrapPayload
         * @return
         */
        public Configuration setWrapPayload(final boolean wrapPayload) {
            this.wrapPayload = wrapPayload;
            return this;
        }

        public String getOpenapiVersion() {
            return openapiVersion;
        }

        public Configuration setOpenapiVersion(final String openapiVersion) {
            this.openapiVersion = openapiVersion;
            return this;
        }

        public Function<Jsonb, OpenRPC> getOpenRPCLoader() {
            return openRPCLoader;
        }

        public Configuration setOpenRPC(final OpenRPC openRPC) {
            this.openRPCLoader = j -> openRPC;
            return this;
        }

        public Configuration setOpenRPCLoader(final Function<Jsonb, OpenRPC> openRPCLoader) {
            this.openRPCLoader = openRPCLoader;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Configuration setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Configuration setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getVersion() {
            return version;
        }

        public Configuration setVersion(String version) {
            this.version = version;
            return this;
        }

        public JsonArray getServers() {
            return servers;
        }

        public Configuration setServers(JsonArray servers) {
            this.servers = servers;
            return this;
        }
    }
}
