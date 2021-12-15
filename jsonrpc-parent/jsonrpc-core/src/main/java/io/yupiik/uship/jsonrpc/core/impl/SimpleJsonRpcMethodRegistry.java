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
import io.yupiik.uship.backbone.johnzon.jsonschema.Schema;
import io.yupiik.uship.backbone.johnzon.jsonschema.SchemaProcessor;
import io.yupiik.uship.backbone.reflect.Reflections;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcError;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import io.yupiik.uship.jsonrpc.core.lang.Tuple2;
import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import io.yupiik.uship.jsonrpc.core.protocol.JsonRpcException;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class SimpleJsonRpcMethodRegistry {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Object[] emptyArgs = new Object[0];
    private final JsonRpcError[] emptyExceptionArray = new JsonRpcError[0];

    private final Map<String, JsonRpcMethodRegistration> handlers = new ConcurrentHashMap<>();

    private String baseUrl;
    private Jsonb jsonb;
    private JsonProvider jsonProvider;
    private Collection<Object> jsonRpcInstances;

    private OpenRPC openRPC;
    private Object2JsonSerializer toJsonValue;

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setJsonb(final Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    public void setJsonProvider(final JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    public void setJsonRpcInstances(final Collection<Object> jsonRpcInstances) {
        this.jsonRpcInstances = jsonRpcInstances;
    }

    public void init() {
        toJsonValue = new Object2JsonSerializer(jsonb);
        jsonRpcInstances.forEach(this::registerMethodFromService);
        registerOpenRPCMethod("openrpc");
        openRPC = doCreateOpenRpc();
    }

    public Map<String, JsonRpcMethodRegistration> getHandlers() {
        return handlers;
    }

    public Unregisterable registerMethod(final Registration registration) {
        final BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> objectToArgs = mapObjectParams(registration.parameters());
        final BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> arrayToArgs = mapArrayParams(registration.parameters());
        final Map<Class<? extends Throwable>, Integer> handledEx =
                ofNullable(registration.exceptionMappings()).stream()
                        .flatMap(Collection::stream)
                        .flatMap(ex -> ofNullable(ex.types()).stream()
                                .flatMap(Collection::stream)
                                .map(e -> new AbstractMap.SimpleEntry<>(e, ex.code())))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Function<Throwable, JsonRpcException> exceptionMapper = exception -> handleException(handledEx, exception);
        final boolean completionStage = isCompletionStage(registration.returnedType());
        final BiFunction<JsonStructure, Tuple2<HttpServletRequest, HttpServletResponse>, Object> invoke = (parameters, servlet) ->
                doInvoke(registration.invoker(), objectToArgs, arrayToArgs, exceptionMapper, parameters, servlet);
        final Function<Object, JsonValue> resultMapper = createResultMapper(completionStage ?
                ParameterizedType.class.cast(registration.returnedType()).getActualTypeArguments()[0] :
                registration.returnedType());
        final BiFunction<JsonStructure, Tuple2<HttpServletRequest, HttpServletResponse>, CompletionStage<JsonValue>> handler = !completionStage ?
                invoke
                        .andThen(resultMapper)
                        .andThen(CompletableFuture::completedFuture) :
                invoke.andThen(stage -> ((CompletionStage<?>) stage)
                        .handle((result, error) -> {
                            if (error == null) {
                                return resultMapper.apply(result);
                            }
                            throw exceptionMapper.apply(
                                    CompletionException.class.isInstance(error) && error.getCause() != null ? error.getCause() : error);
                        }));
        final String jsonRpcMethod = registration.jsonRpcMethod();
        final JsonRpcMethodRegistration existing = handlers.get(jsonRpcMethod);
        if (existing != null && (existing.registration == null || existing.registration.method() == null || registration.method() == null)) {
            throw new IllegalArgumentException("Ambiguous method: '" + jsonRpcMethod + "'");
        } else if (existing != null && existing.registration.method() != registration.method() &&
                registration.method().getDeclaringClass().isAssignableFrom(existing.registration.method().getDeclaringClass())) {
            return () -> { // we ignore this registration then
            };
        }
        logger.info(() -> "Registering '" + registration.jsonRpcMethod() + "'" + ofNullable(registration.clazz())
                .map(c -> (" (" + c.getName() + "." + registration.method().getName() + ')'))
                .orElse("") +
                (existing != null ? " (override)" : ""));
        final JsonRpcMethodRegistration jsonRpcMethodRegistration = new JsonRpcMethodRegistration(registration, handler);
        handlers.put(jsonRpcMethod, jsonRpcMethodRegistration);
        return () -> {
            handlers.remove(jsonRpcMethod, jsonRpcMethodRegistration);
            logger.info(() -> "Unregistered '" + jsonRpcMethod + "'");
        };
    }

    public Unregisterable registerMethodReflect(final Object bean, final Method method,
                                                final JsonRpcMethod config, final JsonRpcParam[] params,
                                                final JsonRpcError[] exceptions) {
        if (!method.canAccess(bean)) {
            method.setAccessible(true);
        }
        final Parameter[] types = method.getParameters();
        final AtomicInteger paramIdx = new AtomicInteger(0);
        final Class<?> declaringClass = extractClass(bean);
        final String methodId = of(config.name())
                .filter(it -> !it.isEmpty())
                .orElse(method.getDeclaringClass().getName() + "." + method.getName());
        return registerMethod(new Registration(
                declaringClass, method,
                methodId,
                method.getGenericReturnType(),
                args -> doInvoke(bean, method, args),
                Stream.of(params)
                        .map(p -> {
                            final int idx = paramIdx.getAndIncrement();
                            final Optional<JsonRpcParam> param = ofNullable(params[idx]);
                            return new Registration.Parameter(
                                    Reflections.resolveType(types[idx].getParameterizedType(), declaringClass),
                                    param.map(JsonRpcParam::value).filter(it -> !it.isBlank()).orElseGet(types[idx]::getName),
                                    idx,
                                    param.map(JsonRpcParam::required).orElse(false),
                                    param.map(JsonRpcParam::documentation).orElse(""));
                        })
                        .collect(toList()),
                Stream.of(exceptions)
                        .map(e -> new Registration.ExceptionMapping(
                                Stream.of(e.handled()).collect(toList()),
                                e.code(),
                                e.documentation()))
                        .collect(toList()), config.documentation()));
    }

    private Class<?> extractClass(final Object bean) {
        Class<?> c = bean == null ? Object.class : bean.getClass();
        while (c != null && c.getName().contains("$$")) {
            c = c.getSuperclass();
        }
        return c == null ? Object.class : c;
    }

    protected OpenRPC doCreateOpenRpc() {
        final var info = doCreateOpenRpcInfo();
        final var components = new OpenRPC.Components(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());

        try (final var schemaProcessor = new SchemaProcessor(false, false, v -> jsonb.fromJson(v, Schema.class))) {
            final var componentsSchemaProcessorCache = new SchemaProcessor.InMemoryCache() {
                @Override
                public void onSchemaCreated(final Class<?> type, final Schema schema) {
                    super.onSchemaCreated(type, schema);
                    final String ref = findRef(type);
                    if (ref != null) {
                        components.getSchemas().putIfAbsent(ref.substring(getRefPrefix().length()), schema);
                    }
                }

                @Override
                protected String getRefPrefix() {
                    return "#/components/schemas/";
                }
            };
            final var methods = handlers.entrySet().stream()
                    .map(handler -> toRpcMethod(schemaProcessor, componentsSchemaProcessorCache, handler))
                    .collect(toList());
            return new OpenRPC("1.2.4", info, toServers(), methods, components);
        }
    }

    protected List<OpenRPC.Server> toServers() {
        return List.of(new OpenRPC.Server(
                "api",
                getBaseUrl(),
                "JSON-RPC API", Map.of()));
    }

    protected OpenRPC.RpcMethod toRpcMethod(final SchemaProcessor schemaProcessor, final SchemaProcessor.InMemoryCache componentsSchemaProcessorCache,
                                            final Map.Entry<String, JsonRpcMethodRegistration> handler) {
        final var reg = handler.getValue().registration();
        final var result = new OpenRPC.Value(
                handler.getKey() + "__result",
                null,
                schemaProcessor.mapSchemaFromClass(unwrapType(reg.returnedType()), componentsSchemaProcessorCache),
                null, null);

        final var errors = reg.exceptionMappings() != null ?
                reg.exceptionMappings().stream()
                        .map(it -> new OpenRPC.ErrorValue(
                                it.code(),
                                it.documentation(),
                                it.types() != null && !it.types().isEmpty() ?
                                        schemaProcessor.mapSchemaFromClass(it.types().iterator().next(), componentsSchemaProcessorCache)
                                        : null))
                        .collect(toList()) : null;
        final var params = reg.parameters() != null ?
                reg.parameters().stream()
                        .filter(p -> p.type() != HttpServletRequest.class && p.type() != HttpServletResponse.class)
                        .map(it -> new OpenRPC.Value(
                                it.name(), it.documentation(),
                                schemaProcessor.mapSchemaFromClass(it.type(), componentsSchemaProcessorCache),
                                it.required(), null))
                        .collect(toList()) : null;
        return new OpenRPC.RpcMethod(
                handler.getKey(), List.of(), reg.documentation(), reg.documentation(), List.of(), params,
                result, null, null, errors, List.of(), "either", null);
    }

    private Type unwrapType(final Type returnedType) {
        if (ParameterizedType.class.isInstance(returnedType)) {
            final var pt = ParameterizedType.class.cast(returnedType);
            if (Class.class.isInstance(pt.getRawType())) {
                final var raw = Class.class.cast(pt.getRawType());
                if (CompletionStage.class.isAssignableFrom(raw) || Optional.class == raw) {
                    return pt.getActualTypeArguments()[0];
                }
            }
            return pt;
        }
        return returnedType;
    }

    protected OpenRPC.Info doCreateOpenRpcInfo() {
        return new OpenRPC.Info("1.0", "JSON-RPC", null, null, null);
    }

    protected String getBaseUrl() {
        return baseUrl;
    }

    public Unregisterable registerOpenRPCMethod(final String methodId) {
        return registerMethod(new Registration(
                null, null,
                requireNonNull(methodId, "Method can't be null"),
                OpenRPC.class, ignored -> openRPC, emptyList(), emptyList(),
                "Returns the Open-RPC specification."));
    }

    public void registerMethodFromService(final Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz.getName().contains("$$")) { // proxy
            clazz = clazz.getSuperclass();
        }
        registerMethodFromService(clazz == null ? instance.getClass() : clazz, instance);
    }

    public void registerMethodFromService(final Class<?> type, final Object instance) {
        Stream.of(type.getMethods())
                .filter(m -> m.isAnnotationPresent(JsonRpcMethod.class))
                .forEach(method -> registerMethodReflect(
                        instance, method,
                        method.getAnnotation(JsonRpcMethod.class),
                        Stream.of(method.getParameters())
                                .map(p -> p.getAnnotation(JsonRpcParam.class))
                                .toArray(JsonRpcParam[]::new),
                        ofNullable(method.getAnnotationsByType(JsonRpcError.class))
                                .orElse(emptyExceptionArray)));
    }

    private Object doInvoke(final Object bean, final Method method, final Object[] args) {
        try {
            return method.invoke(bean, args);
        } catch (final IllegalAccessException e) {
            throw new JsonRpcException(-32601, "Method can't be called", e);
        } catch (final InvocationTargetException ite) {
            final Throwable targetException = ite.getTargetException();
            if (JsonRpcException.class.isInstance(targetException)) {
                throw JsonRpcException.class.cast(targetException);
            }
            if (RuntimeException.class.isInstance(targetException)) {
                throw RuntimeException.class.cast(targetException);
            }
            if (Error.class.isInstance(targetException)) {
                throw Error.class.cast(targetException);
            }
            throw new IllegalStateException(targetException);
        }
    }

    private JsonRpcException handleException(final Map<Class<? extends Throwable>, Integer> handledEx, final Throwable exception) {
        return JsonRpcException.class.isInstance(exception) ?
                JsonRpcException.class.cast(exception) :
                handledEx.entrySet().stream()
                        .filter(handled -> handled.getKey().isAssignableFrom(exception.getClass()))
                        .findFirst()
                        .map(handled -> new JsonRpcException(handled.getValue(), exception.getMessage(), exception))
                        .orElseGet(() -> new JsonRpcException(-32603, exception.getMessage(), exception));
    }

    private Function<Object, JsonValue> createResultMapper(final Type genericReturnType) {
        if (ParameterizedType.class.isInstance(genericReturnType) &&
                ParameterizedType.class.cast(genericReturnType).getRawType() == Optional.class) {
            final Function<Object, JsonValue> nestedMapper = createResultMapper(
                    ParameterizedType.class.cast(genericReturnType).getActualTypeArguments()[0]);
            return v -> v == null || !Optional.class.cast(v).isPresent() ? null : nestedMapper.apply(Optional.class.cast(v).get());
        }
        if (Class.class.isInstance(genericReturnType)) {
            final Class<?> clazz = Class.class.cast(genericReturnType);
            if (CharSequence.class.isAssignableFrom(clazz)) {
                return v -> v == null ? null : jsonProvider.createValue(String.valueOf(v));
            }
            if (Integer.class.isAssignableFrom(clazz) || int.class == clazz) {
                return v -> v == null ? null : jsonProvider.createValue(Integer.class.cast(v));
            }
            if (Double.class.isAssignableFrom(clazz) || double.class == clazz) {
                return v -> v == null ? null : jsonProvider.createValue(Double.class.cast(v));
            }
            if (Boolean.class.isAssignableFrom(clazz) || boolean.class == clazz) {
                return v -> v == null ? null : (Boolean.TRUE.equals(v) ? JsonValue.TRUE : JsonValue.FALSE);
            }
        }
        return v -> v == null ? null : toJsonValue.apply(v);
    }

    private Object doInvoke(final Function<Object[], Object> invoker,
                            final BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> objectToArgs,
                            final BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> arrayToArgs,
                            final Function<Throwable, JsonRpcException> exceptionMapper,
                            final JsonStructure parameters,
                            final Tuple2<HttpServletRequest, HttpServletResponse> servlet) {
        final Object[] args = ofNullable(parameters)
                .map(p -> {
                    switch (p.getValueType()) {
                        case OBJECT:
                            return objectToArgs.apply(p.asJsonObject(), servlet);
                        case ARRAY:
                            return arrayToArgs.apply(p.asJsonArray(), servlet);
                        default:
                            throw new JsonRpcException(-32601, "Unsupported params type: " + p.getValueType());
                    }
                })
                .orElseGet(() -> arrayToArgs.apply(JsonValue.EMPTY_JSON_ARRAY, servlet));
        try {
            return invoker.apply(args);
        } catch (final RuntimeException e) {
            throw exceptionMapper.apply(e);
        }
    }

    private BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> mapArrayParams(
            final Collection<Registration.Parameter> params) {
        if (params == null) {
            return (r, http) -> emptyArgs;
        }
        return optimize(params.stream()
                .map(param -> {
                    if (param.type() == HttpServletRequest.class) {
                        return (BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (json, http) -> http.first();
                    }
                    if (param.type() == HttpServletResponse.class) {
                        return (BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (json, http) -> http.second();
                    }
                    final boolean optional = isOptional(param.type());
                    final BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, JsonValue> jsExtractor = (request, http) -> request.size() > param.position() ?
                            request.get(param.position()) : null;
                    final BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, JsonValue> validatedExtractor = !optional && param.required() ?
                            (request, http) -> {
                                final var applied = jsExtractor.apply(request, http);
                                if (applied == null) {
                                    throw new JsonRpcException(-32601, "Missing #" + param.position() + " parameter.");
                                }
                                return applied;
                            } :
                            jsExtractor;
                    return (BiFunction<JsonArray, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (request, http) ->
                            mapToType(param.type(), optional, validatedExtractor.apply(request, http));
                })
                .collect(toList()));
    }

    private BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> mapObjectParams(
            final Collection<Registration.Parameter> params) {
        if (params == null) {
            return (a, b) -> emptyArgs;
        }
        return optimize(params.stream()
                .map(param -> {
                    if (param.type() == HttpServletRequest.class) {
                        return (BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (json, http) -> http.first();
                    }
                    if (param.type() == HttpServletResponse.class) {
                        return (BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (json, http) -> http.second();
                    }

                    final boolean optional = isOptional(param.type());

                    final BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, JsonValue> jsExtractor = (request, http) -> request.get(param.name());
                    final BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, JsonValue> validatedExtractor = !optional && param.required() ?
                            (request, http) -> {
                                final JsonValue applied = jsExtractor.apply(request, http);
                                if (applied == null) {
                                    throw new JsonRpcException(-32601, "Missing '" + param.name() + "' parameter.");
                                }
                                return applied;
                            } :
                            jsExtractor;
                    return (BiFunction<JsonObject, Tuple2<HttpServletRequest, HttpServletResponse>, Object>) (request, http) ->
                            mapToType(param.type(), optional, validatedExtractor.apply(request, http));
                })
                .collect(toList()));
    }

    private <A extends JsonStructure> BiFunction<A, Tuple2<HttpServletRequest, HttpServletResponse>, Object[]> optimize(
            final Collection<BiFunction<A, Tuple2<HttpServletRequest, HttpServletResponse>, Object>> mappers) {
        switch (mappers.size()) {
            case 0:
                return (a, b) -> emptyArgs;
            case 1:
                return mappers.iterator().next().andThen(r -> new Object[]{r});
            default:
                return (p, r) -> mappers.stream().map(fn -> fn.apply(p, r)).toArray(Object[]::new);
        }
    }

    private Object mapToType(final Type expectedType, final boolean optional, final JsonValue apply) {
        if (apply == null) {
            return optional ? empty() : null;
        }
        if (apply.getValueType() == JsonValue.ValueType.NULL) {
            if (expectedType == int.class) {
                return 0;
            }
            if (expectedType == long.class) {
                return 0L;
            }
            if (expectedType == double.class) {
                return 0.;
            }
            if (expectedType == boolean.class) {
                return false;
            }
            return null;
        }
        if (expectedType == String.class && apply.getValueType() == JsonValue.ValueType.STRING) {
            return JsonString.class.cast(apply).getString();
        }
        if ((expectedType == long.class || expectedType == Long.class) && apply.getValueType() == JsonValue.ValueType.NUMBER) {
            return JsonNumber.class.cast(apply).longValue();
        }
        if ((expectedType == int.class || expectedType == Integer.class) && apply.getValueType() == JsonValue.ValueType.NUMBER) {
            return JsonNumber.class.cast(apply).intValue();
        }
        if ((expectedType == double.class || expectedType == Double.class) && apply.getValueType() == JsonValue.ValueType.NUMBER) {
            return JsonNumber.class.cast(apply).doubleValue();
        }
        if ((expectedType == boolean.class || expectedType == Boolean.class) &&
                (apply.getValueType() == JsonValue.ValueType.TRUE || apply.getValueType() == JsonValue.ValueType.FALSE)) {
            return JsonValue.TRUE.equals(apply);
        }
        if (Class.class.isInstance(expectedType) && Class.class.cast(expectedType).isEnum() && apply.getValueType() == JsonValue.ValueType.STRING) {
            return Enum.valueOf(Class.class.cast(expectedType), JsonString.class.cast(apply).getString());
        }
        return jsonb.fromJson(apply.toString(), expectedType);
    }

    private boolean isCompletionStage(final Type expectedType) {
        if (ParameterizedType.class.isInstance(expectedType)) {
            final Type rawType = ParameterizedType.class.cast(expectedType).getRawType();
            return Class.class.isInstance(rawType) && CompletionStage.class.isAssignableFrom(Class.class.cast(rawType));
        }
        return false;
    }

    private boolean isOptional(final Type expectedType) {
        return ParameterizedType.class.isInstance(expectedType) &&
                ParameterizedType.class.cast(expectedType).getRawType() == Optional.class;
    }

    @FunctionalInterface
    public interface Unregisterable extends AutoCloseable {
        void close();
    }

    public static class JsonRpcMethodRegistration {
        private final Registration registration;
        private final BiFunction<JsonStructure, Tuple2<HttpServletRequest, HttpServletResponse>, CompletionStage<JsonValue>> executor;

        public JsonRpcMethodRegistration(final Registration registration,
                                         final BiFunction<JsonStructure, Tuple2<HttpServletRequest, HttpServletResponse>, CompletionStage<JsonValue>> executor) {
            this.registration = registration;
            this.executor = executor;
        }

        public Registration registration() {
            return registration;
        }

        public BiFunction<JsonStructure, Tuple2<HttpServletRequest, HttpServletResponse>, CompletionStage<JsonValue>> executor() {
            return executor;
        }
    }
}
