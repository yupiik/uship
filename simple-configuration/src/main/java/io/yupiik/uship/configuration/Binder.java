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
package io.yupiik.uship.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class Binder {
    private static final String UNSET = "yupiik.binder.unset";

    private final String prefix;
    private final List<String> args;

    public Binder(final String prefix, final List<String> args) {
        this.prefix = prefix;
        this.args = args;
    }

    public <T> T bind(final Class<T> instance) {
        try {
            return bind(instance.getConstructor().newInstance());
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public <T> T bind(final T instance) {
        bind(instance, instance.getClass());
        return instance;
    }

    private void bind(final Object instance, final Class<?> type) {
        if (type == Object.class || type == null) {
            return;
        }
        Stream.of(type.getDeclaredFields())
                .filter(this::isParam)
                .sorted(comparing(it -> toName(it, getParam(it))))
                .forEach(param -> {
                    final var conf = getParam(param);
                    final var paramName = toName(param, conf);
                    doBind(instance, param, conf, paramName);
                });
        bind(instance, type.getSuperclass());
    }

    protected Param getParam(final Field it) {
        return it.getAnnotation(Param.class);
    }

    protected boolean isParam(Field it) {
        return it.isAnnotationPresent(Param.class);
    }

    protected String toName(final Field param, final Param conf) {
        return (prefix == null || prefix.isBlank() ? "" : prefix + "-") + (conf.name().isBlank() ? param.getName() : conf.name());
    }

    protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
        final var value = findParam(paramName);
        final Object toSet;
        if (isList(param)) {
            final var listType = ParameterizedType.class.cast(param.getGenericType()).getActualTypeArguments()[0];
            final var list = value.flatMap(this::splitListValue).map(it -> coerce(it, Class.class.cast(listType))).collect(toList());
            if (list.isEmpty()) { // try env
                final var env = getenv(toEnvKey(paramName));
                if (env != null) {
                    list.addAll(splitListValue(env).map(it -> coerce(it, Class.class.cast(listType))).collect(toList()));
                }
            }
            if (list.isEmpty()) {
                if (!param.canAccess(instance)) {
                    param.setAccessible(true);
                }
                Object defaultValue = null;
                try {
                    defaultValue = param.get(instance);
                } catch (final IllegalAccessException e) {
                    // no-op
                }
                toSet = defaultValue;
                if (conf.required() && (toSet == null || Collection.class.cast(toSet).isEmpty())) {
                    throw new IllegalArgumentException("Missing parameter --" + paramName);
                }
            } else {
                toSet = list;
            }
        } else { // singular value
            final var fieldType = param.getType().getTypeName();
            if (isNestedModel(instance, fieldType)) {
                toSet = newNestedBinder(paramName, args).bind(param.getType());
            } else { // "primitive"
                toSet = value.findFirst()
                        .map(it -> coerce(it, param.getType()))
                        .orElseGet(() -> {
                            final var env = getenv(toEnvKey(paramName));
                            if (env != null) {
                                return coerce(env, param.getType());
                            }
                            if (conf.required()) {
                                throw new IllegalArgumentException("Missing parameter --" + paramName);
                            }
                            // let's keep field's default
                            return null;
                        });
            }
        }
        if (toSet != null) {
            if (!param.canAccess(instance)) {
                param.setAccessible(true);
            }
            try {
                param.set(instance, toSet);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected String getenv(final String key) {
        final var env = System.getenv(key);
        return UNSET.equals(env) ? null : env;
    }

    protected String toEnvKey(String paramName) {
        return paramName.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
    }

    protected Binder newNestedBinder(final String paramName, final List<String> args) {
        return new Binder(paramName, args) {
            @Override // enables to keep the overridden behavior when the root binder is a child
            protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                Binder.this.doBind(instance, param, conf, paramName);
            }
        };
    }

    protected Stream<String> splitListValue(final String it) {
        return Stream.of(it.split(","));
    }

    protected boolean isList(final Field param) {
        return ParameterizedType.class.isInstance(param.getGenericType()) &&
                List.class == ParameterizedType.class.cast(param.getGenericType()).getRawType();
    }

    // todo: relax?
    protected boolean isNestedModel(final Object instance, final String fieldType) {
        return !isEnum(instance, fieldType) &&
                fieldType.startsWith(instance.getClass().getPackageName()) || // assume nested classes are in the same package
                fieldType.endsWith("Configuration"); // convention
    }

    private boolean isEnum(final Object instance, final String fieldType) {
        try {
            return ofNullable(instance.getClass().getClassLoader())
                    .orElseGet(Thread.currentThread()::getContextClassLoader)
                    .loadClass(fieldType.strip())
                    .isEnum();
        } catch (final Exception | Error e) {
            return false;
        }
    }

    private Object coerce(final String value, final Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        }
        if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }
        if (type == String.class || type == Object.class) {
            return value;
        }
        if (type == Path.class) {
            return Paths.get(value);
        }
        if (type.isEnum()) {
            return Enum.valueOf(Class.class.cast(type), value.strip());
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    protected Stream<String> findParam(final String name) {
        final var result = new ArrayList<String>();
        final var neg = "--no-" + name;
        final var expected = List.of("--" + name, "-" + name);
        for (int i = 0; i < args.size(); i++) {
            final var current = args.get(i);
            if (expected.contains(current)) {
                if (args.size() - 1 == i) {
                    result.add("false");
                } else {
                    final var value = args.get(i + 1);
                    if (!UNSET.equals(value)) {
                        result.add(value);
                    }
                    i++;
                }
            } else if (neg.equals(current)) {
                result.add("false");
            }
        }
        return result.stream();
    }

    public static <T> T bindPrefixed(final T instance) {
        return bindPrefixed(System.getProperties(), instance);
    }

    public static <T> T bindPrefixed(final Properties properties, final T instance) {
        final var prefix = instance.getClass().getAnnotation(Prefix.class).value() + '-';
        return new Binder(null, properties.stringPropertyNames().stream()
                .filter(it -> it.startsWith(prefix))
                .flatMap(it -> Stream.of("--" + it, properties.getProperty(it)))
                .collect(toList()))
                .bind(instance);
    }
}
