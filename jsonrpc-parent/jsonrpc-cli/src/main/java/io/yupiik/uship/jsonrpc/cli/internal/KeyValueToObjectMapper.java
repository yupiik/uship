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
package io.yupiik.uship.jsonrpc.cli.internal;

import jakarta.enterprise.context.ApplicationScoped;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * field1.field2=value for direct primitives
 * field1.array2.0.field3 = value for arrays
 * field1.map2.0.key = keyX && field1.map2.0.value.field3 = valueOfField3 for maps
 * <p>
 * IMPORTANT: this class MUST NOT log since it is used in Milkyway main before we start.
 */
@ApplicationScoped
public class KeyValueToObjectMapper {
    private final ConcurrentMap<Class<?>, ClassBinder<?>> classes = new ConcurrentHashMap<>();

    private static BiConsumer<Object, Object> setWithField(final Field field) {
        return (instance, value) -> {
            try {
                field.set(instance, value);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static BiConsumer<Object, Object> setWithSetter(final Method method) {
        return (instance, value) -> {
            try {
                method.invoke(instance, value);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        };
    }

    public void clear(final Class<?> key) {
        classes.remove(key);
    }

    public <T> Bindable<T> getOrCreate(final Class<T> type) {
        return getOrCreateClassBinder(type, null);
    }

    /**
     * Create a bindable instance.
     *
     * @param type     the type to be able to bind.
     * @param fallback a function returning if the not bindable field must be ignored.
     * @param <T>      the expected type bindable type.
     * @return the bindable.
     */
    public <T> Bindable<T> getOrCreate(final Class<T> type,
                                       final Function<AccessibleObject, Boolean> fallback) {
        return getOrCreateClassBinder(type, fallback);
    }

    private <T> ClassBinder<T> getOrCreateClassBinder(final Class<T> type,
                                                      final Function<AccessibleObject, Boolean> fallback) {
        ClassBinder<?> model = classes.get(type);
        if (model == null) {
            final Map<String, Binder> fields = extractFields(type)
                    .filter(it -> isNotExcluded(it.getType()))
                    .peek(f -> f.setAccessible(true))
                    .collect(toMap(Field::getName, f -> {
                        try {
                            return createBinder(setWithField(f), f.getGenericType(), f.getName(), fallback, findCustomConverter(f));
                        } catch (final IllegalArgumentException iae) {
                            return handleFallback(fallback, f, iae);
                        }
                    }, (s1, s2) -> s1));
            // also handle public setters to have kind of virtual fields
            fields.putAll(Stream.of(type.getMethods())
                    .filter(it -> it.getReturnType() == void.class)
                    .filter(it -> it.getParameterCount() == 1)
                    .filter(it -> isNotExcluded(it.getParameterTypes()[0]))
                    .filter(it -> it.getName().startsWith("set") && it.getName().length() >= 4)
                    .filter(it -> !fields.containsKey(mapSetterToFieldName(it)))
                    .collect(toMap(this::mapSetterToFieldName, setter -> {
                        try {
                            return createBinder(
                                    setWithSetter(setter), setter.getGenericParameterTypes()[0],
                                    mapSetterToFieldName(setter), fallback, null);
                        } catch (final IllegalArgumentException iae) {
                            return handleFallback(fallback, setter, iae);
                        }
                    })));
            final Constructor<?> defaultConstructor = getDefaultConstructor(type);
            model = new ClassBinder<>(defaultConstructor, defaultConstructor == null ? "No constructor found for " + type : null, fields);
            final ClassBinder<?> old = classes.putIfAbsent(type, model);
            if (old != null) {
                model = old;
            }
        }
        return (ClassBinder<T>) model;
    }

    protected Function<String, Object> findCustomConverter(final Field field) {
        return null;
    }

    private boolean isNotExcluded(final Class<?> it) { // we know these types and don't bind them
        return it != ClassLoader.class && (!it.isInterface() || (Map.class.isAssignableFrom(it) || Collection.class.isAssignableFrom(it)));
    }

    private Binder handleFallback(final Function<AccessibleObject, Boolean> fallback,
                                  final AccessibleObject f,
                                  final IllegalArgumentException original) {
        if (fallback != null && fallback.apply(f)) {
            return new ClassBinder<>(null, "No constructor found for " + f, emptyMap());
        }
        throw original;
    }

    private String mapSetterToFieldName(Method it) {
        return Character.toLowerCase(it.getName().charAt(3)) +
                (it.getName().length() > 4 ? it.getName().substring(4) : "");
    }

    public Binder createBinder(final BiConsumer<Object, Object> setter,
                               final Type genericType, final String name,
                               final Function<AccessibleObject, Boolean> fallback,
                               final Function<String, Object> customConverter) {
        if (isConverterPrimitive(genericType)) {
            return new PrimitiveFieldBinder(customConverter == null ?
                    this::convert :
                    (type, o) -> customConverter.apply(String.valueOf(o)), setter, name, (Class<?>) genericType);
        }
        if (Properties.class == genericType) {
            return new PropertiesFieldBinder(this::convert, setter, name);
        }
        if (genericType instanceof ParameterizedType) {
            final var pt = (ParameterizedType) genericType;
            if (pt.getRawType() instanceof Class) {
                final var clazz = (Class<?>) pt.getRawType();
                final var args = pt.getActualTypeArguments();
                if (Collection.class.isAssignableFrom(clazz) && args.length == 1) {
                    final var argType = args[0];
                    if (argType instanceof Class) {
                        final var componentType = (Class<?>) argType;
                        final var collector = Set.class.isAssignableFrom(clazz) ? toSet() : toList();
                        if (isConverterPrimitive(componentType)) {
                            return new PrimitiveCollectedFieldBinder(this::convert, setter, name, componentType, collector);
                        }
                        return new CollectedFieldBinder(setter, name, getOrCreate(componentType, fallback), collector);
                    }
                } else if (Map.class == clazz && args.length == 2) {
                    if (String.class != args[0]) {
                        throw new IllegalArgumentException("Map keys can only be String (to be JSON friendly) - " + name + "/" + genericType);
                    }
                    if (args[1] instanceof Class) {
                        final var componentType = (Class<?>) args[1];
                        if (isConverterPrimitive(componentType)) {
                            return new PrimitiveMapFieldBinder(this::convert, setter, name, componentType);
                        }
                        return new ObjectMapFieldBinder(getOrCreate(componentType, fallback), setter, name);
                    }
                    if (args[1] instanceof WildcardType) { // assume for now
                        return new PrimitiveMapFieldBinder(this::convert, setter, name, String.class);
                    }
                }
            }
        }
        if (genericType instanceof Class) {
            final var clazz = (Class<?>) genericType;
            if (clazz.isArray()) {
                final Class<?> componentType = clazz.getComponentType();
                final IntFunction<Object> factory = i -> Array.newInstance(componentType, i);
                if (isConverterPrimitive(componentType)) {
                    return new PrimitiveArrayFieldBinder(this::convert, setter, factory, name, componentType);
                }
                return new ArrayFieldBinder(setter, name, getOrCreate(componentType, fallback), factory);
            }
            final var delegate = getOrCreateClassBinder(clazz, fallback);
            return new PrefixedDelegatingBinder(setter, name, delegate);
        }
        throw new IllegalArgumentException("Unsupported type: " + setter);
    }

    private boolean isConverterPrimitive(final Type genericType) {
        return Stream.of(
                        int.class, long.class, byte.class, short.class, boolean.class, float.class, double.class,
                        Integer.class, Long.class, Byte.class, Short.class, Boolean.class, Float.class, Double.class,
                        String.class)
                .anyMatch(t -> genericType == t) || (genericType instanceof Class && ((Class<?>) genericType).isEnum());
    }

    private <T> Constructor<T> getDefaultConstructor(final Class<T> type) {
        try {
            final Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    private Stream<Field> extractFields(final Class<?> clazz) {
        return Stream.concat(
                Stream.of(clazz.getDeclaredFields())
                        .filter(it -> !Modifier.isStatic(it.getModifiers()))
                        .filter(it -> !it.isSynthetic()),
                clazz.getSuperclass() == null || clazz.getSuperclass() == Object.class ?
                        Stream.empty() : extractFields(clazz.getSuperclass()));
    }

    private Object convert(final Class<?> to, final Object value) {
        if (to.isInstance(value)) {
            return value;
        }
        if (value == null) {
            return getDefault(to);
        }
        final String string = String.valueOf(value);
        if (string.isEmpty()) {
            return getDefault(to);
        }
        if (int.class == to || Integer.class == to) {
            return Double.valueOf(string.trim()).intValue();
        }
        if (long.class == to || Long.class == to) {
            return Double.valueOf(string.trim()).longValue();
        }
        if (byte.class == to || Byte.class == to) {
            return Double.valueOf(string.trim()).byteValue();
        }
        if (short.class == to || Short.class == to) {
            return Double.valueOf(string.trim()).shortValue();
        }
        if (boolean.class == to || Boolean.class == to) {
            return Boolean.parseBoolean(string.trim());
        }
        if (char.class == to || Character.class == to) {
            return string.charAt(0);
        }
        if (double.class == to || Double.class == to) {
            return Double.parseDouble(string.trim());
        }
        if (float.class == to || Float.class == to) {
            return Double.valueOf(string.trim()).floatValue();
        }
        if (to.isEnum()) {
            return Enum.valueOf((Class) to, string);
        }
        throw new IllegalArgumentException("Unsupported type: " + to);
    }

    private Object getDefault(final Class<?> to) {
        if (int.class == to) {
            return 0;
        }
        if (long.class == to) {
            return 0L;
        }
        if (byte.class == to) {
            return (byte) 0;
        }
        if (short.class == to) {
            return (short) 0;
        }
        if (boolean.class == to) {
            return false;
        }
        if (char.class == to) {
            return (char) 0;
        }
        if (double.class == to) {
            return 0.;
        }
        if (float.class == to) {
            return 0.f;
        }
        return null;
    }

    public interface Bindable<T> {
        T bind(Map<String, ?> configuration);

        T bind(T instance, Map<String, ?> configuration);
    }

    public interface Binder {
        void set(Object instance, Map<String, ?> value);

        default <T> Supplier<Map<String, T>> mapSupplier(final Map<String, ?> configuration) {
            return configuration instanceof SortedMap ?
                    () -> new TreeMap<>(((SortedMap<String, T>) configuration).comparator()) : HashMap::new;
        }

        default Predicate<String> startsWith(final Map<String, ?> configuration, final String prefix) {
            return configuration instanceof SortedMap && SortedMap.class.cast(configuration).comparator() == String.CASE_INSENSITIVE_ORDER ?
                    new Predicate<>() {
                        private final String lowercasePrefix = prefix.toLowerCase(ROOT);

                        @Override
                        public boolean test(final String s) {
                            return s != null && s.toLowerCase(ROOT).startsWith(lowercasePrefix);
                        }
                    } : s -> s != null && s.startsWith(prefix);
        }
    }

    private static class ClassBinder<T> implements Binder, Bindable<T> {
        private final Constructor<T> constructor;
        private final String error;
        private final Map<String, Binder> binder;

        private ClassBinder(final Constructor<T> constructor, final String error, final Map<String, Binder> binder) {
            this.constructor = constructor;
            this.error = error;
            this.binder = binder;
        }

        @Override
        public void set(final Object instance, final Map<String, ?> value) {
            binder.forEach((name, binder) -> binder.set(instance, value));
        }

        @Override
        public T
        bind(final Map<String, ?> configuration) {
            try {
                if (constructor == null) {
                    throw new IllegalStateException(error);
                }
                return bind(constructor.newInstance(), configuration);
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalArgumentException(e.getTargetException());
            }
        }

        @Override
        public T bind(final T instance, final Map<String, ?> configuration) {
            set(instance, configuration);
            return instance;
        }
    }

    private static class PrimitiveFieldBinder implements Binder {
        private final BiFunction<Class<?>, Object, Object> converter;
        private final BiConsumer<Object, Object> setter;
        private final String name;
        private final Class<?> type;

        private PrimitiveFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                     final BiConsumer<Object, Object> setter, final String name,
                                     final Class<?> type) {
            this.converter = converter;
            this.setter = setter;
            this.name = name;
            this.type = type;
        }

        @Override
        public void set(final Object instance, final Map<String, ?> configuration) {
            final Object value = configuration.get(name);
            if (value == null) {
                return;
            }
            setter.accept(
                    instance,
                    converter.apply(type, value));
        }
    }

    private static abstract class BaseArrayFieldBinder implements Binder {
        private final BiConsumer<Object, Object> setter;
        private final String prefix;

        private BaseArrayFieldBinder(final BiConsumer<Object, Object> setter, final String name) {
            this.setter = setter;
            this.prefix = name + ".";
        }

        @Override
        public void set(final Object instance, final Map<String, ?> configuration) {
            final Predicate<String> startsWith = startsWith(configuration, prefix);
            final Map<String, ?> subConfig = configuration.keySet().stream()
                    .filter(startsWith)
                    .collect(toMap(identity(), configuration::get, (a, b) -> a, mapSupplier(configuration)));
            if (subConfig.isEmpty()) {
                return;
            }
            final String lenKey = prefix + "length";
            final Object length = configuration.get(lenKey); // can be forced
            final int len = length == null ?
                    Integer.MAX_VALUE : (length instanceof Number ?
                    ((Number) length).intValue() : Integer.parseInt(String.valueOf(length)));
            final Map<Integer, ? extends Map<String, ?>> groups = subConfig.entrySet().stream()
                    .filter(e -> !lenKey.equals(e.getKey()))
                    .collect(groupingBy(e -> {
                        final String key = e.getKey().substring(prefix.length());
                        final int sep = key.indexOf('.');
                        return Integer.parseInt(key.substring(0, sep < 0 ? key.length() : sep));
                    }, toMap(e -> {
                        final String key = e.getKey().substring(prefix.length());
                        final int sep = key.indexOf('.');
                        return sep < 0 ? "" : key.substring(sep + 1);
                    }, Map.Entry::getValue, (a, b) -> a, mapSupplier(configuration))));
            final Stream<?> arrayStream = groups.entrySet().stream()
                    .filter(e -> e.getKey() < len)
                    .sorted(comparing(Map.Entry::getKey))
                    .map(e -> bind(e.getValue()));
            setter.accept(instance, collect(arrayStream));
        }

        protected abstract Object bind(Map<String, ?> value);

        protected abstract Object collect(Stream<?> arrayStream);
    }

    private static abstract class BaseMapFieldBinder<T> implements Binder {
        private final BiConsumer<Object, Object> setter;
        private final String prefix;

        private BaseMapFieldBinder(final BiConsumer<Object, Object> setter, final String name) {
            this.setter = setter;
            this.prefix = name + ".";
        }

        @Override
        public void set(final Object instance, final Map<String, ?> configuration) {
            final Predicate<String> startsWith = startsWith(configuration, prefix);
            final Map<String, ?> subConfig = configuration.keySet().stream()
                    .filter(startsWith)
                    .collect(toMap(identity(), configuration::get, (a, b) -> a, mapSupplier(configuration)));
            if (subConfig.isEmpty()) {
                return;
            }
            final String lenKey = prefix + "length";
            final Object length = configuration.get(lenKey);
            final int len = length == null ?
                    Integer.MAX_VALUE : (length instanceof Number ?
                    ((Number) length).intValue() : Integer.parseInt(String.valueOf(length)));
            final Map<Integer, ? extends Map<String, ?>> groups = subConfig.entrySet().stream()
                    .filter(e -> !lenKey.equals(e.getKey()))
                    .collect(groupingBy(e -> {
                        final String key = e.getKey().substring(prefix.length());
                        final int sep = key.indexOf('.');
                        return Integer.parseInt(key.substring(0, sep < 0 ? key.length() : sep));
                    }, toMap(e -> {
                        final String key = e.getKey().substring(prefix.length());
                        final int sep = key.indexOf('.');
                        return sep < 0 ? "" : key.substring(sep + 1);
                    }, Map.Entry::getValue, (a, b) -> a, mapSupplier(configuration))));

            final Predicate<String> startsWithValue = startsWith(configuration, "value.");
            final Stream<Map.Entry<String, ?>> arrayStream = groups.entrySet().stream()
                    .filter(e -> e.getKey() < len && e.getValue().containsKey("key"))
                    .sorted(comparing(Map.Entry::getKey))
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(
                            String.valueOf(e.getValue().get("key")),
                            bind(e.getValue().entrySet().stream()
                                    .filter(it -> startsWithValue.test(it.getKey()) || "value".equalsIgnoreCase(it.getKey()))
                                    .map(it -> new AbstractMap.SimpleImmutableEntry<>(
                                            "value".equalsIgnoreCase(it.getKey()) ?
                                                    "" : it.getKey().substring("value.".length()),
                                            it.getValue()))
                                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, mapSupplier(configuration))))));
            setter.accept(instance, doCollect(arrayStream));
        }

        protected abstract T doCollect(final Stream<Map.Entry<String, ?>> arrayStream);

        protected abstract Object bind(Map<String, ?> value);
    }

    private static abstract class BasePrimitiveArrayFieldBinder extends BaseArrayFieldBinder {
        private final BiFunction<Class<?>, Object, Object> converter;
        private final Class<?> itemType;

        private BasePrimitiveArrayFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                              final BiConsumer<Object, Object> setter, final String name,
                                              final Class<?> itemType) {
            super(setter, name);
            this.converter = converter;
            this.itemType = itemType;
        }

        @Override
        protected Object bind(final Map<String, ?> value) {
            return converter.apply(itemType, value.isEmpty() ? null : value.values().iterator().next());
        }
    }

    private static class PropertiesFieldBinder extends BaseMapFieldBinder<Properties> {
        private final BiFunction<Class<?>, Object, Object> converter;

        private PropertiesFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                      final BiConsumer<Object, Object> setter, final String name) {
            super(setter, name);
            this.converter = converter;
        }

        @Override
        protected Object bind(final Map<String, ?> value) {
            return converter.apply(String.class, value.isEmpty() ? null : value.values().iterator().next());
        }

        @Override
        protected Properties doCollect(final Stream<Map.Entry<String, ?>> arrayStream) {
            return arrayStream.collect(Collector.of(
                    Properties::new,
                    (properties, entry) -> properties.put(entry.getKey(), String.valueOf(entry.getValue())),
                    (p1, p2) -> {
                        p1.putAll(p2);
                        return p1;
                    }));
        }
    }

    private static class PrimitiveMapFieldBinder extends BaseMapFieldBinder<Map<String, ?>> {
        private final BiFunction<Class<?>, Object, Object> converter;
        private final Class<?> itemType;

        private PrimitiveMapFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                        final BiConsumer<Object, Object> setter, final String name,
                                        final Class<?> itemType) {
            super(setter, name);
            this.converter = converter;
            this.itemType = itemType;
        }

        @Override
        protected Object bind(final Map<String, ?> value) {
            return converter.apply(itemType, value.isEmpty() ? null : value.values().iterator().next());
        }

        @Override
        protected Map<String, ?> doCollect(final Stream<Map.Entry<String, ?>> arrayStream) {
            return arrayStream.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private static class ObjectMapFieldBinder extends BaseMapFieldBinder<Map<String, ?>> {
        private final Bindable<?> delegate;

        private ObjectMapFieldBinder(final Bindable<?> delegate, final BiConsumer<Object, Object> setter,
                                     final String name) {
            super(setter, name);
            this.delegate = delegate;
        }

        @Override
        protected Object bind(final Map<String, ?> value) {
            return delegate.bind(value);
        }

        @Override
        protected Map<String, ?> doCollect(final Stream<Map.Entry<String, ?>> arrayStream) {
            return arrayStream.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private static class PrimitiveArrayFieldBinder extends BasePrimitiveArrayFieldBinder {
        private final IntFunction factory;

        private PrimitiveArrayFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                          final BiConsumer<Object, Object> setter, final IntFunction<?> arrayFactory,
                                          final String name, final Class<?> itemType) {
            super(converter, setter, name, itemType);
            this.factory = arrayFactory;
        }

        @Override
        protected Object collect(final Stream<?> arrayStream) {
            return arrayStream.toArray(factory);
        }
    }

    private static class PrimitiveCollectedFieldBinder extends BasePrimitiveArrayFieldBinder {
        private final Collector<?, ?, ?> collector;

        private PrimitiveCollectedFieldBinder(final BiFunction<Class<?>, Object, Object> converter,
                                              final BiConsumer<Object, Object> setter, final String name,
                                              final Class<?> itemType, final Collector<?, ?, ?> collector) {
            super(converter, setter, name, itemType);
            this.collector = collector;
        }

        @Override
        protected Object collect(final Stream<?> arrayStream) {
            return Stream.class.cast(arrayStream).collect(collector);
        }
    }

    private static abstract class BaseObjectArrayFieldBinder extends BaseArrayFieldBinder {
        private final Bindable<?> delegate;

        private BaseObjectArrayFieldBinder(final BiConsumer<Object, Object> setter, final String name,
                                           final Bindable<?> delegate) {
            super(setter, name);
            this.delegate = delegate;
        }

        @Override
        protected Object bind(final Map<String, ?> value) {
            return delegate.bind(value);
        }
    }

    private static class ArrayFieldBinder extends BaseObjectArrayFieldBinder {
        private final IntFunction factory;

        private ArrayFieldBinder(final BiConsumer<Object, Object> setter, final String name,
                                 final Bindable<?> delegate, final IntFunction<?> arrayFactory) {
            super(setter, name, delegate);
            this.factory = arrayFactory;
        }

        @Override
        protected Object collect(final Stream<?> arrayStream) {
            return arrayStream.toArray(factory);
        }
    }

    private static class CollectedFieldBinder extends BaseObjectArrayFieldBinder {
        private final Collector<?, ?, ?> collector;

        private CollectedFieldBinder(final BiConsumer<Object, Object> setter, final String name,
                                     final Bindable<?> delegate, final Collector<?, ?, ?> collector) {
            super(setter, name, delegate);
            this.collector = collector;
        }

        @Override
        protected Object collect(final Stream<?> arrayStream) {
            return Stream.class.cast(arrayStream).collect(collector);
        }
    }

    private static class PrefixedDelegatingBinder implements Binder {
        private final BiConsumer<Object, Object> setter;
        private final ClassBinder<?> delegate;
        private final String prefix;

        private PrefixedDelegatingBinder(final BiConsumer<Object, Object> setter,
                                         final String name,
                                         final ClassBinder<?> delegate) {
            this.setter = setter;
            this.delegate = delegate;
            this.prefix = name + '.';
        }

        @Override
        public void set(final Object instance, final Map<String, ?> value) {
            final Predicate<String> startsWith = startsWith(value, prefix);
            final Map<String, ?> nested = value.keySet().stream()
                    .filter(startsWith)
                    .collect(toMap(k -> k.substring(prefix.length()), value::get, (a, b) -> a, mapSupplier(value)));
            if (!nested.isEmpty()) {
                setter.accept(instance, delegate.bind(nested));
            }
        }
    }
}
