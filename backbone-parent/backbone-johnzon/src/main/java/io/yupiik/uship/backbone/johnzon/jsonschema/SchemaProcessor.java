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
package io.yupiik.uship.backbone.johnzon.jsonschema;

import io.yupiik.uship.backbone.johnzon.jsonschema.api.JsonSchema;
import io.yupiik.uship.backbone.johnzon.jsonschema.api.JsonSchemaMetadata;
import io.yupiik.uship.backbone.reflect.Reflections;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

public class SchemaProcessor implements AutoCloseable {
    private final Class<?> persistenceCapable;
    private final boolean setClassAsTitle;
    private final boolean useReflectionForDefaults;
    private final Function<String, Schema> schemaReader;

    public SchemaProcessor() {
        this(false, false, null);
    }

    public SchemaProcessor(final boolean setClassAsTitle, final boolean useReflectionForDefaults) {
        this(setClassAsTitle, useReflectionForDefaults, null);
    }

    public SchemaProcessor(final boolean setClassAsTitle, final boolean useReflectionForDefaults, final Function<String, Schema> schemaReader) {
        this.setClassAsTitle = setClassAsTitle;
        this.useReflectionForDefaults = useReflectionForDefaults;
        this.schemaReader = schemaReader == null ? new LazySchemaReader() : schemaReader;

        Class<?> pc = null;
        try {
            pc = Thread.currentThread().getContextClassLoader()
                    .loadClass("org.apache.openjpa.enhance.PersistenceCapable");
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            // no-op
        }
        persistenceCapable = pc;
    }

    public Schema mapSchemaFromClass(final Type model) {
        return mapSchemaFromClass(model, new InMemoryCache());
    }

    public Schema mapSchemaFromClass(final Type model, final Cache cache) {
        final ReflectionValueExtractor reflectionValueExtractor = useReflectionForDefaults ? new ReflectionValueExtractor() : null;
        return doMapSchemaFromClass(model, cache, reflectionValueExtractor, useReflectionForDefaults ? reflectionValueExtractor.createInstance(model) : null);
    }

    private Schema doMapSchemaFromClass(final Type model, final Cache cache,
                                        final ReflectionValueExtractor reflectionValueExtractor,
                                        final Instance instance) {
        final Schema schema = new Schema();
        fillSchema(model, schema, cache, reflectionValueExtractor, instance);
        return schema;
    }

    public void fillSchema(final Type rawModel, final Schema schema, final Cache cache,
                           final ReflectionValueExtractor reflectionValueExtractor,
                           final Instance instance) {
        final Type model = unwrapType(rawModel);
        if (Class.class.isInstance(model)) {
            if (boolean.class == model) {
                schema.setType(Schema.SchemaType.bool);
            } else if (Boolean.class == model) {
                schema.setType(Schema.SchemaType.bool);
                schema.setNullable(true);
            } else if (String.class == model || JsonString.class == model) {
                schema.setType(Schema.SchemaType.string);
            } else if (double.class == model || float.class == model) {
                schema.setType(Schema.SchemaType.number);
            } else if (Double.class == model || Float.class == model || JsonNumber.class == model) {
                schema.setType(Schema.SchemaType.number);
                schema.setNullable(true);
            } else if (int.class == model || short.class == model || byte.class == model || long.class == model) {
                schema.setType(Schema.SchemaType.integer);
            } else if (Integer.class == model || Short.class == model || Byte.class == model || Long.class == model) {
                schema.setType(Schema.SchemaType.integer);
                schema.setNullable(true);
            } else if (JsonObject.class == model || JsonValue.class == model || JsonStructure.class == model) {
                schema.setType(Schema.SchemaType.object);
                schema.setNullable(true);
                schema.setProperties(new HashMap<>());
            } else if (JsonArray.class == model) {
                schema.setType(Schema.SchemaType.array);
                schema.setNullable(true);
                final Schema items = new Schema();
                items.setType(Schema.SchemaType.object);
                items.setProperties(new HashMap<>());
                schema.setItems(items);
            } else if (isStringable(model)) {
                schema.setType(Schema.SchemaType.string);
                schema.setNullable(true);
            } else {
                Class<?> from = Class.class.cast(model);
                final var provided = from.getAnnotation(JsonSchema.class);
                if (provided != null) {
                    if (!provided.value().isEmpty()) {
                        forward(schemaReader.apply(provided.value()), schema);
                        return;
                    } else if (provided.type() != JsonSchema.class) {
                        from = provided.type();
                    }
                }
                if (from.isEnum()) {
                    schema.setId(from.getName().replace('.', '_').replace('$', '_'));
                    schema.setType(Schema.SchemaType.string);
                    schema.setEnumeration(asList(from.getEnumConstants()));
                    schema.setNullable(true);
                } else if (from.isArray()) {
                    schema.setType(Schema.SchemaType.array);
                    final Schema items = new Schema();
                    fillSchema(from.getComponentType(), items, cache, reflectionValueExtractor, instance);
                    schema.setItems(items);
                } else if (Collection.class.isAssignableFrom(from)) {
                    schema.setType(Schema.SchemaType.array);
                    final Schema items = new Schema();
                    fillSchema(Object.class, items, cache, reflectionValueExtractor, instance);
                    schema.setItems(items);
                } else {
                    schema.setType(Schema.SchemaType.object);
                    getOrCreateReusableObjectComponent(from, schema, cache, reflectionValueExtractor, instance);
                }
            }
        } else {
            if (ParameterizedType.class.isInstance(model)) {
                final ParameterizedType pt = ParameterizedType.class.cast(model);
                if (Class.class.isInstance(pt.getRawType()) && Map.class.isAssignableFrom(Class.class.cast(pt.getRawType()))) {
                    schema.setType(Schema.SchemaType.object);
                    getOrCreateReusableObjectComponent(Object.class, schema, cache, reflectionValueExtractor, instance);
                } else if (pt.getActualTypeArguments().length == 1 && Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                    schema.setType(Schema.SchemaType.array);
                    final Schema items = new Schema();
                    final Class<?> type = Class.class.cast(pt.getActualTypeArguments()[0]);
                    final Instance item;
                    if (instance != null && Collection.class.isInstance(instance.value) && !Collection.class.cast(instance.value).isEmpty()) {
                        item = new Instance(Collection.class.cast(instance.value).iterator().next(), instance.isCreated());
                    } else {
                        item = null;
                    }
                    fillSchema(type, items, cache, reflectionValueExtractor, item);
                    schema.setItems(items);
                } else {
                    schema.setType(Schema.SchemaType.array);
                }
            } else if (TypeVariable.class.isInstance(model)) {
                schema.setType(Schema.SchemaType.object);
            } else { // todo?
                schema.setType(Schema.SchemaType.array);
                schema.setItems(new Schema());
            }
        }
    }

    private void getOrCreateReusableObjectComponent(final Class<?> from, final Schema schema,
                                                    final Cache cache,
                                                    final ReflectionValueExtractor reflectionValueExtractor,
                                                    final Instance instance) {
        schema.setType(Schema.SchemaType.object);
        final String ref = cache.findRef(from);
        if (ref != null) {
            schema.setRef(ref);
            cache.initDefinitions(from);
            return;
        } else if (Object.class == from) {
            schema.setProperties(new HashMap<>());
            return;
        }
        if (setClassAsTitle) {
            schema.setTitle(from.getName());
        }

        final BiPredicate<Type, String> ignored = createIgnorePredicate(from);

        cache.onClass(from);
        schema.setProperties(new HashMap<>());
        Class<?> current = from;
        while (current != null && current != Object.class) {
            final Map<String, String> fields = Stream.of(current.getDeclaredFields())
                    .filter(it -> isVisible(it, it.getModifiers()))
                    .peek(f -> {
                        if (Modifier.isFinal(f.getModifiers())) {
                            handleRequired(schema, () -> findFieldName(f));
                        }
                    })
                    .peek(f -> {
                        final String fieldName = findFieldName(f);
                        if (!ignored.test(f.getGenericType(), fieldName)) {
                            final Instance fieldInstance;
                            if (reflectionValueExtractor != null) {
                                fieldInstance = reflectionValueExtractor.createDemoInstance(
                                        instance == null ? null : instance.value, f);
                            } else {
                                fieldInstance = null;
                            }
                            final Schema value = doMapSchemaFromClass(Reflections.resolveType(f.getGenericType(), from), cache, reflectionValueExtractor, fieldInstance);
                            fillMeta(f, value);
                            if (fieldInstance != null && !fieldInstance.isCreated()) {
                                switch (value.getType()) {
                                    case array:
                                    case object:
                                        break;
                                    default:
                                        value.setDefaultValue(fieldInstance.value);
                                }
                            }
                            schema.getProperties().put(fieldName, value);
                        } else {
                            onIgnored(schema, f, cache);
                        }
                    }).collect(toMap(Field::getName, this::findFieldName));
            Stream.of(current.getDeclaredMethods())
                    .filter(it -> isVisible(it, it.getModifiers()))
                    .filter(it -> (it.getName().startsWith("get") || it.getName().startsWith("is")) && it.getName().length() > 2)
                    .forEach(m -> {
                        final String methodName = findMethodName(m);
                        final String key = fields.getOrDefault(methodName, methodName); // ensure we respect jsonbproperty on fields
                        if (!ignored.test(Reflections.resolveType(m.getGenericReturnType(), from), key) && !schema.getProperties().containsKey(key)) {
                            schema.getProperties().put(key, doMapSchemaFromClass(m.getGenericReturnType(), cache, null, null));
                        }
                    });
            current = current.getSuperclass();
        }
        cache.onSchemaCreated(from, schema);
    }

    protected void fillMeta(final Field f, final Schema schema) {
        ofNullable(f.getAnnotation(JsonSchemaMetadata.class))
                .or(() -> findRecordJsonSchemaMetadata(f))
                .ifPresent(s -> {
                    of(s.title()).filter(it -> !it.isEmpty()).ifPresent(schema::setTitle);
                    of(s.description()).filter(it -> !it.isEmpty()).ifPresent(schema::setDescription);
                });
        ofNullable(f.getAnnotation(Deprecated.class)).map(it -> true).ifPresent(schema::setDeprecated);
    }

    private Optional<JsonSchemaMetadata> findRecordJsonSchemaMetadata(final Field field) {
        return Stream.of(field.getDeclaringClass().getConstructors())
                .flatMap(c -> Stream.of(c.getParameters()))
                .filter(p -> Objects.equals(p.getName(), field.getName()) &&
                        Objects.equals(field.getGenericType(), p.getParameterizedType()) &&
                        p.isAnnotationPresent(JsonSchemaMetadata.class))
                .findFirst()
                .map(p -> p.getAnnotation(JsonSchemaMetadata.class));
    }

    protected void onIgnored(final Schema schema, final Field f, final Cache cache) {
        // no-op
    }

    protected BiPredicate<Type, String> createIgnorePredicate(final Class<?> from) {
        return persistenceCapable != null && persistenceCapable.isAssignableFrom(from) ?
                (t, v) -> v.startsWith("pc") : (t, v) -> false;
    }

    private boolean isVisible(final AnnotatedElement elt, final int modifiers) {
        return !Modifier.isStatic(modifiers) && !elt.isAnnotationPresent(JsonbTransient.class);
    }

    private Type unwrapType(final Type rawModel) {
        if (ParameterizedType.class.isInstance(rawModel)) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(rawModel);
            if (Stream.of(parameterizedType.getActualTypeArguments()).allMatch(WildcardType.class::isInstance)) {
                return parameterizedType.getRawType();
            }
            if (Class.class.isInstance(parameterizedType.getRawType()) &&
                    CompletionStage.class.isAssignableFrom(Class.class.cast(parameterizedType.getRawType()))) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return rawModel;
    }

    protected boolean isStringable(final Type model) {
        return Date.class == model || model.getTypeName().startsWith("java.time.") ||
                Class.class == model || Type.class == model ||
                BigDecimal.class == model || BigInteger.class == model;
    }

    private void handleRequired(final Schema schema, final Supplier<String> nameSupplier) {
        if (schema.getRequired() == null) {
            schema.setRequired(new ArrayList<>());
        }
        final String name = nameSupplier.get();
        if (!schema.getRequired().contains(name)) {
            schema.getRequired().add(name);
        }
    }

    private String findFieldName(final Field f) {
        if (f.isAnnotationPresent(JsonbProperty.class)) {
            return f.getAnnotation(JsonbProperty.class).value();
        }
        // getter
        final String fName = f.getName();
        final String subName = Character.toUpperCase(fName.charAt(0))
                + (fName.length() > 1 ? fName.substring(1) : "");
        try {
            final Method getter = f.getDeclaringClass().getMethod("get" + subName);
            if (getter.isAnnotationPresent(JsonbProperty.class)) {
                return getter.getAnnotation(JsonbProperty.class).value();
            }
        } catch (final NoSuchMethodException e) {
            if (boolean.class == f.getType()) {
                try {
                    final Method isser = f.getDeclaringClass().getMethod("is" + subName);
                    if (isser.isAnnotationPresent(JsonbProperty.class)) {
                        return isser.getAnnotation(JsonbProperty.class).value();
                    }
                } catch (final NoSuchMethodException e2) {
                    // no-op
                }
            }
        }
        return fName;
    }

    private String findMethodName(final Method m) {
        if (m.isAnnotationPresent(JsonbProperty.class)) {
            return m.getAnnotation(JsonbProperty.class).value();
        }
        final String name = m.getName();
        if (name.startsWith("get")) {
            return decapitalize(name.substring("get".length()));
        }
        if (name.startsWith("is")) {
            try {
                m.getDeclaringClass().getDeclaredField(name);
                return name;
            } catch (final NoSuchFieldException nsme) {
                return decapitalize(name.substring("is".length()));
            }
        }
        return decapitalize(name);
    }

    private String decapitalize(final String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public void close() {
        if (AutoCloseable.class.isInstance(schemaReader)) {
            try {
                AutoCloseable.class.cast(schemaReader).close();
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void forward(final Schema loaded, final Schema schema) {
        schema.setDefinitions(loaded.getDefinitions());
        schema.setType(loaded.getType());
        schema.setProperties(loaded.getProperties());
        schema.setAdditionalProperties(loaded.getAdditionalProperties());
        schema.setAllOf(loaded.getAllOf());
        schema.setAnyOf(loaded.getAnyOf());
        schema.setDefaultValue(loaded.getDefaultValue());
        schema.setDeprecated(loaded.getDeprecated());
        schema.setDescription(loaded.getDescription());
        schema.setEnumeration(loaded.getEnumeration());
        schema.setExample(loaded.getExample());
        schema.setExclusiveMaximum(loaded.getExclusiveMaximum());
        schema.setExclusiveMinimum(loaded.getExclusiveMinimum());
        schema.setFormat(loaded.getFormat());
        schema.setItems(loaded.getItems());
        schema.setMaxItems(loaded.getMaxItems());
        schema.setMaxLength(loaded.getMaxLength());
        schema.setMaxProperties(loaded.getMaxProperties());
        schema.setMinItems(loaded.getMinItems());
        schema.setMinLength(loaded.getMinLength());
        schema.setMinProperties(loaded.getMinProperties());
        schema.setMaximum(loaded.getMaximum());
        schema.setMinimum(loaded.getMinimum());
        schema.setMultipleOf(loaded.getMultipleOf());
        schema.setNot(loaded.getNot());
        schema.setNullable(loaded.getNullable());
        schema.setOneOf(loaded.getOneOf());
        schema.setPattern(loaded.getPattern());
        schema.setReadOnly(loaded.getReadOnly());
        schema.setRef(loaded.getRef());
        schema.setId(loaded.getId());
        schema.setSchema(loaded.getSchema());
        schema.setRequired(loaded.getRequired());
        schema.setTitle(loaded.getTitle());
        schema.setUniqueItems(loaded.getUniqueItems());
        schema.setWriteOnly(loaded.getWriteOnly());
    }

    /*
    private Object toType(final String s, final Schema.SchemaType type) {
        if (type == null || s.isEmpty()) {
            return null;
        }
        switch (type) {
            case string:
                return s;
            case integer:
                return Integer.valueOf(s);
            case number:
                return Double.valueOf(s);
            case bool:
                return Boolean.parseBoolean(s);
            case object:
            case array:
                // todo using jsonb
            default:
                return null;
        }
    }
     */

    public interface Cache {
        String findRef(Class<?> type);

        void onClass(Class<?> type);

        void onSchemaCreated(Class<?> type, Schema schema);

        void initDefinitions(Class<?> from);
    }

    public static class InMemoryCache implements Cache {
        private final Map<Class<?>, String> refs = new HashMap<>();

        private final Map<Class<?>, Schema> schemas = new HashMap<>();
        private final Map<String, Schema> definitions = new HashMap<>();

        public Map<Class<?>, Schema> getSchemas() {
            return schemas;
        }

        public Map<String, Schema> getDefinitions() {
            return definitions;
        }

        @Override
        public String findRef(final Class<?> type) {
            if (type != Object.class) {
                return refs.get(type);
            }
            return null;
        }

        @Override
        public void onClass(final Class<?> type) {
            refs.putIfAbsent(type, sanitize(type));
        }

        @Override
        public void onSchemaCreated(final Class<?> type, final Schema schema) {
            if (schemas.putIfAbsent(type, schema) == null) {
                if (schema.getId() == null) {
                    final String ref = findRef(type);
                    if (ref != null) {
                        schema.setId(ref.substring(getRefPrefix().length()));
                    }
                }
            } else if (schema.getRef() == null) {
                final String ref = findRef(type);
                if (ref != null) {
                    schema.setRef(ref);
                }
            }
        }

        @Override
        public void initDefinitions(final Class<?> from) { // we add it only if reuse since some editor don't accept that
            if (from == Object.class) {
                return;
            }
            ofNullable(schemas.get(from)).ifPresent(s -> definitions.put(findRef(from).substring(getRefPrefix().length()), s));
        }

        private String sanitize(final Class<?> type) {
            return getRefPrefix() + type.getName().replace('$', '_').replace('.', '_');
        }

        protected String getRefPrefix() {
            return "#/definitions/";
        }
    }

    public static class ReflectionValueExtractor {
        private Instance createDemoInstance(final Object rootInstance, final Field field) {
            if (rootInstance != null && field != null) {
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    final Object value = field.get(rootInstance);
                    if (value != null) {
                        return new Instance(value, false);
                    }
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }

            final Type javaType = field.getGenericType();
            if (Class.class.isInstance(javaType)) {
                return new Instance(tryCreatingObjectInstance(javaType), true);
            } else if (ParameterizedType.class.isInstance(javaType)) {
                final ParameterizedType pt = ParameterizedType.class.cast(javaType);
                final Type rawType = pt.getRawType();
                if (Class.class.isInstance(rawType) && Collection.class.isAssignableFrom(Class.class.cast(rawType))
                        && pt.getActualTypeArguments().length == 1
                        && Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                    final Object instance = tryCreatingObjectInstance(pt.getActualTypeArguments()[0]);
                    final Class<?> collectionType = Class.class.cast(rawType);
                    if (Set.class == collectionType) {
                        return new Instance(singleton(instance), true);
                    }
                    if (SortedSet.class == collectionType) {
                        return new Instance(new TreeSet<>(singletonList(instance)), true);
                    }
                    if (List.class == collectionType || Collection.class == collectionType) {
                        return new Instance(singletonList(instance), true);
                    }
                    // todo?
                    return null;
                }
            }
            return null;
        }

        private Object tryCreatingObjectInstance(final Type javaType) {
            final Class<?> type = Class.class.cast(javaType);
            if (type.isPrimitive()) {
                if (int.class == type) {
                    return 0;
                }
                if (long.class == type) {
                    return 0L;
                }
                if (double.class == type) {
                    return 0.;
                }
                if (float.class == type) {
                    return 0f;
                }
                if (short.class == type) {
                    return (short) 0;
                }
                if (byte.class == type) {
                    return (byte) 0;
                }
                if (boolean.class == type) {
                    return false;
                }
                throw new IllegalArgumentException("Not a primitive: " + type);
            }
            if (Integer.class == type) {
                return 0;
            }
            if (Long.class == type) {
                return 0L;
            }
            if (Double.class == type) {
                return 0.;
            }
            if (Float.class == type) {
                return 0f;
            }
            if (Short.class == type) {
                return (short) 0;
            }
            if (Byte.class == type) {
                return (byte) 0;
            }
            if (Boolean.class == type) {
                return false;
            }
            if (type.getName().startsWith("java.") || type.getName().startsWith("jakarta.")) {
                return null;
            }
            try {
                return type.getConstructor().newInstance();
            } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException
                           | InvocationTargetException e) {
                // no-op, ignore defaults there
            }
            return null;
        }

        private Instance createInstance(final Type model) {
            if (Class.class.isInstance(model)) {
                try {
                    return new Instance(Class.class.cast(model).getConstructor().newInstance(), true);
                } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException
                               | InvocationTargetException e) {
                    // no-op, ignore defaults there
                }
            }
            return null;
        }
    }

    public static class Instance {
        private final Object value;
        private final boolean created;

        private Instance(final Object value, final boolean created) {
            this.value = value;
            this.created = created;
        }

        public Object getValue() {
            return value;
        }

        public boolean isCreated() {
            return created;
        }
    }

    private static class LazySchemaReader implements Function<String, Schema>, AutoCloseable {
        private volatile Jsonb jsonb;

        @Override
        public Schema apply(final String s) {
            if (jsonb == null) {
                synchronized (this) {
                    if (jsonb == null) {
                        jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.skip-cdi", true));
                    }
                }
            }
            return jsonb.fromJson(s, Schema.class);
        }

        @Override
        public void close() throws Exception {
            if (jsonb != null) {
                jsonb.close();
            }
        }
    }
}

