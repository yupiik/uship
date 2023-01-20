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
package io.yupiik.uship.jsonrpc.cli.api;

import io.yupiik.uship.jsonrpc.cli.internal.HelpCommand;
import io.yupiik.uship.jsonrpc.cli.internal.KeyValueToObjectMapper;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.impl.JsonRpcHandler;
import io.yupiik.uship.jsonrpc.core.impl.JsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.impl.Registration;
import io.yupiik.uship.jsonrpc.core.protocol.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class JsonRpcCliExecutor {
    @Inject
    private JsonRpcMethodRegistry registry;

    @Inject
    @JsonRpc
    private HelpCommand helpCommand;

    @Inject
    @StdOut
    private PrintStream stdout;

    @Inject
    @StdErr
    private PrintStream stderr;

    @Inject
    private JsonRpcHandler handler;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Inject
    private Jsonb jsonb;

    @Inject
    private KeyValueToObjectMapper kvom;

    public CompletionStage<?> execute(final String... args) {
        if (args.length == 0 || !registry.getHandlers().containsKey(args[0])) {
            stderr.println(helpCommand.help(HelpCommand.HelpFormat.TEXT, null));
            return completedFuture(null);
        }
        try {
            final var registration = registry.getHandlers().get(args[0]).registration();
            final var options = Stream.of(args).skip(1).collect(toList());
            return handler.execute(createCommandRequest(args[0], options, registration), null, null)
                    .handle((r, e) -> onResponse(options, r, e));
        } catch (final CliException re) {
            stderr.println(re.getMessage());
            return toFailure(re);
        } catch (final RuntimeException re) {
            re.printStackTrace(stderr);
            return toFailure(re);
        }
    }

    protected Object onResponse(final List<String> options, final Object response, final Throwable exception) {
        if (response instanceof Response) {
            final var jsonRpcResponse = (Response) response;
            final var error = jsonRpcResponse.getError();
            if (error != null && error.getCode() == -32601) { // missing method
                stderr.println(error);
                stderr.println();
                stderr.println(helpCommand.help(HelpCommand.HelpFormat.TEXT, null));
                stderr.flush();
                return response;
            }
            if (error != null) {
                stderr.println("Error #" + error.getCode());
                stderr.println(error.getMessage());
                if (error.getData() != null) {
                    stderr.println(error.getData());
                }
                stderr.flush();
                return response;
            }
            final int silent = options.indexOf("--cli-silent");
            if (silent < 0 || !Boolean.parseBoolean(options.get(silent + 1))) {
                stdout.println(toString(jsonRpcResponse.getResult(), ""));
            }
            stdout.flush();
            final int dump = options.indexOf("--cli-response-dump");
            if (dump >= 0) {
                final var dumpPath = Paths.get(options.get(dump + 1)).normalize();
                final var properties = toProperties(jsonRpcResponse.getResult(), new Properties(), "");
                if (dumpPath.getParent() != null && !Files.exists(dumpPath.getParent())) {
                    try {
                        Files.createDirectories(dumpPath.getParent());
                    } catch (final IOException ioException) {
                        throw new IllegalStateException(ioException);
                    }
                }
                try (final var writer = Files.newBufferedWriter(dumpPath)) {
                    properties.store(writer, "");
                } catch (final IOException ioException) {
                    throw new IllegalStateException(ioException);
                }

                final int dumpOnExit = options.indexOf("--cli-response-dump-delete-on-exit");
                if (dumpOnExit >= 0 && Boolean.parseBoolean(options.get(dumpOnExit + 1))) {
                    dumpPath.toFile().deleteOnExit();
                }
            }
        } else if (exception != null) {
            if (exception instanceof CliException) {
                stderr.println(((CliException) exception).getMessage());
            } else {
                exception.printStackTrace(stderr);
            }
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
            if (exception instanceof Error) {
                throw (Error) exception;
            }
            throw new IllegalStateException(exception);
        }
        return response;
    }

    protected String interpolate(final String key) { // to enhance
        return key;
    }

    private Properties toProperties(final JsonValue result, final Properties properties, final String prefix) {
        if (result == null) {
            return properties;
        }
        switch (result.getValueType()) {
            case STRING:
                properties.setProperty(prefix.isEmpty() ? "value" : prefix, ((JsonString) result).getString());
                break;
            case NUMBER:
                properties.setProperty(prefix.isEmpty() ? "value" : prefix, Double.toString(((JsonNumber) result).doubleValue()));
                break;
            case TRUE:
                properties.setProperty(prefix.isEmpty() ? "value" : prefix, "true");
                break;
            case FALSE:
                properties.setProperty(prefix.isEmpty() ? "value" : prefix, "false");
                break;
            case NULL:
                return properties;
            case ARRAY:
                final AtomicInteger idx = new AtomicInteger();
                result.asJsonArray()
                        .forEach(value -> toProperties(value, properties, prefix + (prefix.isEmpty() ? "" : ".") + idx.getAndIncrement()));
                break;
            case OBJECT:
                result.asJsonObject()
                        .forEach((key, value) -> toProperties(value, properties, prefix + (prefix.isEmpty() ? "" : ".") + key));
                break;
            default:
                throw new IllegalArgumentException("Unsupported result type: " + result);
        }
        return properties;
    }

    protected String toString(final JsonValue value, final String prefix) {
        switch (value.getValueType()) {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                return Double.toString(((JsonNumber) value).doubleValue());
            case TRUE:
            case FALSE:
                return Boolean.toString(JsonValue.TRUE.equals(value));
            case NULL:
                return "<null>";
            case OBJECT:
                return prefix + "\n" + value.asJsonObject().entrySet().stream()
                        .map(it -> prefix + "  " + it.getKey() + ": " + toString(it.getValue(), prefix + " "))
                        .collect(joining("\n"));
            case ARRAY:
                return prefix + "\n" + value.asJsonArray().stream()
                        .map(it -> prefix + "  -" + toString(it, prefix + " "))
                        .collect(joining("\n"));
            default:
                throw new CliException("Unsupported value: " + value);
        }
    }

    private CompletionStage<?> toFailure(final RuntimeException re) {
        final CompletableFuture<?> promise = new CompletableFuture<>();
        promise.completeExceptionally(re);
        return promise;
    }

    private JsonStructure createCommandRequest(final String cmd, final List<String> args,
                                               final Registration registration) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", cmd)
                .add("params", toParams(args, registration))
                .build();
    }

    private JsonObjectBuilder toParams(final List<String> args,
                                       final Registration registration) {
        if (args.size() % 2 != 0) {
            throw new CliException("Arguments parity should be pair (name + value): " + args);
        }

        final var builder = jsonBuilderFactory.createObjectBuilder();
        if (args.isEmpty()) {
            return builder;
        }

        final var indexedArgs = IntStream.range(0, args.size() / 2)
                .boxed()
                .map(it -> new AbstractMap.SimpleImmutableEntry<>(
                        Optional.of(args.get(it * 2)).map(k -> k.startsWith("--") ? k.substring("--".length()) : k).get(),
                        mapValue(interpolate(args.get(2 * it + 1)))))
                .flatMap(this::handleGlobalArgs)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        registration.parameters().stream()
                .filter(it -> indexedArgs.containsKey(it.name()) || indexedArgs.keySet().stream().anyMatch(p -> p.startsWith(it.name())))
                .forEach(p -> {
                    if (p.type() == String.class || (p.type() instanceof Class && ((Class<?>) p.type()).isEnum())) {
                        builder.add(p.name(), indexedArgs.get(p.name()));
                    } else if (p.type() == Integer.class || p.type() == int.class) {
                        builder.add(p.name(), Integer.parseInt(indexedArgs.get(p.name())));
                    } else if (p.type() == Long.class || p.type() == long.class) {
                        builder.add(p.name(), Long.parseLong(indexedArgs.get(p.name())));
                    } else if (p.type() == Double.class || p.type() == double.class) {
                        builder.add(p.name(), Double.parseDouble(indexedArgs.get(p.name())));
                    } else if (p.type() == Boolean.class || p.type() == boolean.class) {
                        builder.add(p.name(), Boolean.parseBoolean(indexedArgs.get(p.name())));
                    } else if (p.type() instanceof ParameterizedType) {
                        final ParameterizedType pt = (ParameterizedType) p.type();
                        if (pt.getRawType() instanceof Class) {
                            final Class<?> rawClass = (Class<?>) pt.getRawType();
                            if (Collection.class.isAssignableFrom(rawClass)) {
                                final Map<String, String> values = new CliMap(filterMap(indexedArgs, p));
                                if (values.isEmpty() && indexedArgs.containsKey(p.name())) {
                                    builder.add(p.name(), jsonb.fromJson(indexedArgs.get(p.name()), JsonArray.class));
                                } else {
                                    final AtomicReference<Object> ref = new AtomicReference<>();
                                    kvom.createBinder((i, v) -> ref.set(v), p.type(), p.name(), f -> false, f -> null)
                                            .set(null, values);
                                    if (ref.get() != null) {
                                        builder.add(p.name(), jsonb.fromJson(jsonb.toJson(ref.get()), JsonArray.class));
                                    }
                                }
                            } else if (Map.class.isAssignableFrom(rawClass)) {
                                final Map<String, String> values = new CliMap(filterMap(indexedArgs, p));
                                if (values.isEmpty() && indexedArgs.containsKey(p.name())) {
                                    builder.add(p.name(), jsonb.fromJson(indexedArgs.get(p.name()), JsonObject.class));
                                } else {
                                    final AtomicReference<Object> ref = new AtomicReference<>();
                                    kvom.createBinder((i, v) -> ref.set(v), p.type(), p.name(), f -> false, f -> null)
                                            .set(null, values);
                                    if (ref.get() != null) {
                                        builder.add(p.name(), jsonb.fromJson(jsonb.toJson(ref.get()), JsonObject.class));
                                    }
                                }
                            } else {
                                throw new IllegalArgumentException("Unsupported command parameter: " + p.type());
                            }
                        } else {
                            throw new IllegalArgumentException("Unsupported command parameter: " + p.type());
                        }
                    } else if (p.type() instanceof Class) { // assume object
                        final var values = new CliMap(filterMap(indexedArgs, p).entrySet().stream()
                                .collect(toMap(e -> e.getKey().substring(p.name().length() + 1), Map.Entry::getValue)));
                        if (values.isEmpty() && indexedArgs.containsKey(p.name())) { // json param
                            builder.add(p.name(), jsonb.fromJson(indexedArgs.get(p.name()), JsonObject.class));
                        } else {
                            final Object instance = kvom.getOrCreate((Class<?>) p.type()).bind(values);
                            builder.add(p.name(), jsonb.fromJson(jsonb.toJson(instance), JsonObject.class));
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported command parameter: " + p.type());
                    }
                });
        return builder;
    }

    private Stream<Map.Entry<String, String>> handleGlobalArgs(final Map.Entry<String, String> entry) {
        if ("cli-env".equals(entry.getKey())) {
            final var path = Paths.get(entry.getValue());
            final var properties = readProperties(path);
            return properties.stringPropertyNames().stream()
                    .map(it -> new AbstractMap.SimpleImmutableEntry<>(it, properties.getProperty(it)));
        }
        return Stream.of(entry);
    }

    private String mapValue(final String value) {
        if (value.startsWith("@") && !value.startsWith("@@")) { // @foo = read foo content, @@foo = @foo string
            try {
                return Files.readString(Paths.get(value.substring(1)));
            } catch (final IOException e) {
                throw new CliException("Invalid file: " + value + ", " +
                        "ensure to escape the first @ with another @ if you intend to pass it as a string.");
            }
        }
        return value;
    }

    private Map<String, String> filterMap(final Map<String, String> indexedArgs, final Registration.Parameter param) {
        final var prefix1 = param.name() + '.';
        final var prefix2 = param.name() + '-';
        return indexedArgs.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix1) || e.getKey().startsWith(prefix2))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Properties readProperties(final Path path) {
        final var properties = new Properties();
        try (final var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    private static class CliMap extends HashMap<String, String> {
        private CliMap(final Map<String, String> values) {
            putAll(values);
        }

        @Override
        public void putAll(final Map<? extends String, ? extends String> m) {
            m.forEach(this::put);
        }

        @Override
        public String put(final String key, final String value) {
            final String otherKey = key.replace('-', '.');
            if (!otherKey.equals(key)) {
                super.put(otherKey, value);
            }
            return super.put(key, value);
        }
    }
}
