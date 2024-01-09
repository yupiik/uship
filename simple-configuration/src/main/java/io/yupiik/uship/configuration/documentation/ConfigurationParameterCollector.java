/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.configuration.documentation;

import io.yupiik.uship.configuration.Binder;
import io.yupiik.uship.configuration.Param;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class ConfigurationParameterCollector implements Supplier<Map<String, ConfigurationParameterCollector.Parameter>> {
    private final List<Class<?>> classes;
    private final BiPredicate<Object, String> isNested;
    private final Function<String, Binder> binderFactory;

    protected ConfigurationParameterCollector(final List<Class<?>> classes, final BiPredicate<Object, String> isNested,
                                              final Function<String, Binder> binderFactory) {
        this.classes = classes;
        this.isNested = isNested;
        this.binderFactory = binderFactory;
    }

    public ConfigurationParameterCollector(final Class<?>... batchClasses) {
        this(List.of(batchClasses));
    }

    public ConfigurationParameterCollector(final List<Class<?>> batchClasses) {
        this(batchClasses, null, null);
    }

    public ConfigurationParameterCollector(final List<Class<?>> batchClasses,
                                           final BiPredicate<Object, String> isNested) {
        this(batchClasses, isNested, null);
    }

    @Override
    public Map<String, Parameter> get() {
        return getWithPrefix(batchType -> batchType.getSimpleName().toLowerCase(Locale.ROOT));
    }

    public Map<String, Parameter> getWithPrefix(final Function<Class<?>, String> prefix) {
        return classes.stream()
                .flatMap(batchType -> {
                    final var doc = new HashMap<String, Parameter>();
                    visitor(prefix.apply(batchType), doc).bind(batchType);
                    return doc.entrySet().stream();
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Binder visitor(final String prefix, final Map<String, Parameter> doc) {
        return new Binder(prefix, List.of()) {
            @Override
            protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                onParam(instance, param, conf, paramName, this::isList, this::isNestedModel, this::newNestedBinder, doc);
            }

            @Override
            protected boolean isNestedModel(final Object instance, final String fieldType) {
                return super.isNestedModel(instance, fieldType) || (isNested != null && isNested.test(instance, fieldType));
            }
        };
    }

    public String toAsciidoc() {
        return "[options=\"header\",cols=\"a,a,2\"]\n" +
                "|===\n" +
                "|Name|Env Variable|Description\n" +
                getWithPrefix(c -> "").entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> "" +
                                "| `" + e.getKey() + "` " + (e.getValue().getParam().required() ? "*" : "") +
                                "| `" + e.getKey().replaceAll("[^A-Za-z0-9]", "_").toUpperCase(ROOT) + "` " +
                                "| " + e.getValue().getParam().description() + "\n")
                        .collect(joining()) + "\n" +
                "|===\n";
    }

    protected void onParam(final Object instance, final Field param, final Param conf, final String paramName,
                           final Predicate<Field> isList, final BiPredicate<Object, String> isNestedModel,
                           final BiFunction<String, List<String>, Binder> newNestedBinder, final Map<String, Parameter> doc) {
        if (isList.test(param) || !isNestedModel.test(instance, param.getType().getTypeName())) {
            if (!param.canAccess(instance)) {
                param.setAccessible(true);
            }
            try {
                final var defValue = param.get(instance);
                doc.put(paramName, new Parameter(
                        conf, defValue == null ? null : String.valueOf(defValue), param.getGenericType(),
                        paramName));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else { // recursive call
            newNestedBinder.apply(paramName, List.of()).bind(param.getType());
        }
    }

    public class Parameter {
        private Param param;
        private String defaultValue;
        private Type type;
        private String name;

        public Parameter(Param param, String defaultValue, Type type, String name) {
            this.param = param;
            this.defaultValue = defaultValue;
            this.type = type;
            this.name = name;
        }

        public Param getParam() {
            return param;
        }

        public void setParam(Param param) {
            this.param = param;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
