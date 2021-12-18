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
package io.yupiik.uship.persistence.impl;

import io.yupiik.uship.persistence.api.Column;
import io.yupiik.uship.persistence.api.Entity;
import io.yupiik.uship.persistence.api.Id;
import io.yupiik.uship.persistence.api.PersistenceException;
import io.yupiik.uship.persistence.api.Table;
import io.yupiik.uship.persistence.api.lifecycle.OnDelete;
import io.yupiik.uship.persistence.api.lifecycle.OnInsert;
import io.yupiik.uship.persistence.api.lifecycle.OnLoad;
import io.yupiik.uship.persistence.api.lifecycle.OnUpdate;
import io.yupiik.uship.persistence.spi.DatabaseTranslation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

// todo: some operation don't need the resultset.metadata usage (in particular queries we build ourselve internally).
public class EntityImpl<E> implements Entity<E> {
    private final DatabaseImpl database;
    private final Class<?> rootType;
    private final Map<String, Field> fields;
    private final List<Field> idFields;
    private final Map<String, ParameterHolder> constructorParameters;
    private final Constructor<E> constructor;
    private final String table;
    private final String findByIdQuery;
    private final String updateQuery;
    private final String deleteQuery;
    private final String insertQuery;
    private final String findAllQuery;
    private final List<Method> onInserts;
    private final List<Method> onUpdates;
    private final List<Method> onDeletes;
    private final List<Method> onLoads;
    private final DatabaseTranslation translation;
    private final List<ColumnMetadata> columns;

    public EntityImpl(final DatabaseImpl database, final Class<E> type, final DatabaseTranslation translation) {
        final var record = Records.isRecord(type);

        this.database = database;
        this.translation = translation;
        this.rootType = type;

        try {
            this.constructor = record ?
                    (Constructor<E>) Stream.of(type.getConstructors())
                            .max(comparing(Constructor::getParameterCount))
                            .orElseThrow() :
                    type.getConstructor();
        } catch (final NoSuchMethodException e) {
            throw new PersistenceException(e);
        }

        final var paramCounter = new AtomicInteger();
        this.constructorParameters = record ?
                captureConstructorParameters(type)
                        .collect(toMap(p -> name(p).orElseGet(p::getName), p -> new ParameterHolder(p, paramCounter.getAndIncrement()), (a, b) -> {
                            throw new IllegalArgumentException("Ambiguous parameter: " + a);
                        }, CaseInsensitiveLinkedHashMap::new)) :
                Map.of();
        this.fields = captureFields(type)
                .collect(toMap(this::name, identity(), (a, b) -> {
                    throw new IllegalArgumentException("Ambiguous field: " + a);
                }, CaseInsensitiveLinkedHashMap::new));

        this.idFields = this.fields.values().stream()
                .filter(it -> it.isAnnotationPresent(Id.class) || ofNullable(constructorParameters.get(it.getName()))
                        .map(p -> p.parameter.isAnnotationPresent(Id.class))
                        .filter(ok -> ok)
                        .isPresent())
                .sorted(comparing(f -> {
                    final var id = f.getAnnotation(Id.class);
                    if (id == null) {
                        return ofNullable(constructorParameters.get(f.getName()))
                                .filter(p -> p.parameter.isAnnotationPresent(Id.class))
                                .map(p -> p.parameter.getAnnotation(Id.class))
                                .map(Id::order)
                                .orElseThrow();
                    }
                    return id.order();
                }))
                .collect(toList());

        this.table = translation.wrapTableName(ofNullable(type.getAnnotation(Table.class))
                .map(Table::value)
                .orElseGet(type::getSimpleName));

        this.onInserts = captureMethods(type, OnInsert.class).collect(toList());
        this.onUpdates = captureMethods(type, OnUpdate.class).collect(toList());
        this.onDeletes = captureMethods(type, OnDelete.class).collect(toList());
        this.onLoads = captureMethods(type, OnLoad.class).collect(toList());

        // todo: go through translation to have escaping if needed, for now assume we don't use keywords in mapping
        final var byIdWhereClause = " WHERE " + idFields.stream()
                .map(f -> translation.wrapFieldName(name(f)) + " = ?")
                .collect(joining(" AND "));
        final var fieldNamesCommaSeparated = fields.values().stream()
                .map(f -> translation.wrapFieldName(name(f)))
                .collect(joining(", "));
        this.findByIdQuery = "" +
                "SELECT " +
                fieldNamesCommaSeparated +
                " FROM " + table +
                byIdWhereClause;
        this.updateQuery = "" +
                "UPDATE " + table + " SET " +
                fields.values().stream().map(f -> translation.wrapFieldName(name(f)) + " = ?").collect(joining(", ")) +
                byIdWhereClause;
        this.deleteQuery = "" +
                "DELETE FROM " + table + byIdWhereClause;
        this.insertQuery = "" +
                "INSERT INTO " + table + " (" + fieldNamesCommaSeparated + ") " +
                "VALUES (" + fields.values().stream().map(f -> "?").collect(joining(", ")) + ")";
        this.findAllQuery = "" +
                "SELECT " + fieldNamesCommaSeparated +
                " FROM " + table;

        this.columns = (constructorParameters.isEmpty() ?
                fields.entrySet().stream()
                        .sorted(fieldOrder())
                        .map(e -> new ColumnMetadataImpl(e.getValue().getAnnotations(), e.getValue().getName(), e.getValue().getGenericType(), e.getKey())) :
                constructorParameters.entrySet().stream()
                        .sorted(constructorOrder())
                        .map(e -> new ColumnMetadataImpl(
                                e.getValue().parameter.getAnnotations(), e.getValue().parameter.getName(),
                                e.getValue().parameter.getParameterizedType(), e.getKey())))
                .distinct()
                .collect(toList());
    }

    @Override
    public String[] ddl() {
        final var fields = columns.stream()
                .map(c -> c.columnName() + " " + type(c)
                        .orElseGet(() -> translation.toDatabaseType(Class.class.cast(c.type()), c.getAnnotations())))
                .collect(joining(", "));
        return new String[]{
                "CREATE TABLE " + table + " (" +
                        fields +
                        (idFields.isEmpty() ? "" : translation.toCreateTablePrimaryKeySuffix(
                                this.fields.entrySet().stream()
                                        .filter(it -> idFields.contains(it.getValue()))
                                        .map(e -> entry(e.getKey(), mergeAnnotations(e.getValue())))
                                        .collect(toList()))) +
                        ")"
        };
    }

    @Override
    public Class<?> getRootType() {
        return rootType;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public String getFindByIdQuery() {
        return findByIdQuery;
    }

    @Override
    public String getUpdateQuery() {
        return updateQuery;
    }

    @Override
    public String getDeleteQuery() {
        return deleteQuery;
    }

    @Override
    public String getInsertQuery() {
        return insertQuery;
    }

    @Override
    public String getFindAllQuery() {
        return findAllQuery;
    }

    @Override
    public List<ColumnMetadata> getOrderedColumns() {
        return columns;
    }

    @Override
    public String concatenateColumns(final ColumnsConcatenationRequest request) {
        final var translation = database.getTranslation();
        return columns.stream()
                .filter(it -> !request.getIgnored().contains(it.javaName()) && !request.getIgnored().contains(it.columnName()))
                .map(e -> {
                    final var name = e.javaName();
                    final var field = request.getPrefix().endsWith(".") ?
                            request.getPrefix() + translation.wrapFieldName(e.columnName()) :
                            translation.wrapFieldName(request.getPrefix() + e.columnName());
                    final var alias = request.getAliasPrefix() != null ?
                            " as " + translation.wrapFieldName(!request.getAliasPrefix().isBlank() ?
                                    request.getAliasPrefix() + Character.toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "") :
                                    e.javaName()) :
                            "";
                    return field + alias;
                })
                .collect(joining(", "));
    }

    public List<Method> getOnInserts() {
        return onInserts;
    }

    public List<Method> getOnUpdates() {
        return onUpdates;
    }

    @Override
    public Function<ResultSet, E> mapFromPrefix(final String prefix, final String... columns) {
        if (prefix == null || prefix.isBlank()) {
            return toProvider(columns);
        }

        final var lcPrefix = prefix.toLowerCase(ROOT);
        return toProvider(Stream.of(columns)
                .map(it -> it.toLowerCase(ROOT).startsWith(lcPrefix) ? it.substring(prefix.length()) : null /* ignored but don't loose the index */)
                .toArray(String[]::new));
    }

    @Override
    public Function<ResultSet, E> mapFromPrefix(final String prefix, final ResultSet resultSet) {
        try {
            return mapFromPrefix(prefix, toNames(resultSet).toArray(String[]::new));
        } catch (final SQLException e) {
            throw new PersistenceException(e);
        }
    }

    public Function<ResultSet, E> nextProvider(final ResultSet resultSet) {
        try {
            return toProvider(toNames(resultSet).toArray(String[]::new));
        } catch (final SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private Function<ResultSet, E> toProvider(final String[] columns) {
        return constructorParameters.isEmpty() ?
                pojoProvider(columns) :
                recordProvider(columns);
    }

    private Function<ResultSet, E> recordProvider(final String[] columns) {
        final var boundParams = new ArrayList<Map.Entry<ParameterHolder, Integer>>();
        final var notSet = new ArrayList<>(constructorParameters.values());
        for (int i = 0; i < columns.length; i++) {
            final var key = columns[i];
            if (key == null) {
                continue;
            }

            var param = constructorParameters.get(key); // fast path if it matches
            if (param == null) { // try by java name
                param = this.columns.stream()
                        .filter(c -> c.javaName().equalsIgnoreCase(key))
                        .findFirst()
                        .map(c -> constructorParameters.get(c.columnName()))
                        .orElse(null);
            }
            if (param != null) {
                notSet.remove(param);
                boundParams.add(entry(param, i + 1));
            }
        }
        return resultSet -> {
            final var params = new Object[constructorParameters.size()];
            try {
                for (final var param : boundParams) {
                    params[param.getKey().index] = database.lookup(resultSet, param.getValue(), param.getKey().parameter.getType());
                }
                if (!notSet.isEmpty()) {
                    notSet.forEach(p -> params[p.index] = p.defaultValue);
                }
                final var instance = constructor.newInstance(params);
                callMethodsWith(onLoads, instance);
                return instance;
            } catch (final SQLException | InstantiationException | IllegalAccessException e) {
                throw new PersistenceException(e);
            } catch (final InvocationTargetException e) {
                throw new PersistenceException(e.getTargetException());
            }
        };
    }

    private Function<ResultSet, E> pojoProvider(final String[] columns) {
        final var boundFields = new ArrayList<Map.Entry<Field, Integer>>();
        for (int i = 0; i < columns.length; i++) {
            final var key = columns[i];
            if (key == null) {
                continue;
            }

            var field = fields.get(key);
            if (field == null) { // try by java name
                field = this.columns.stream()
                        .filter(c -> c.javaName().equalsIgnoreCase(key))
                        .findFirst()
                        .map(c -> fields.get(c.columnName()))
                        .orElse(null);
            }
            if (field != null) {
                boundFields.add(entry(field, i + 1));
            }
        }
        return resultSet -> {
            try {
                final var instance = constructor.newInstance();
                for (final var field : boundFields) {
                    if (field == null) {
                        continue;
                    }
                    field.getKey().set(instance, database.lookup(resultSet, field.getValue(), field.getKey().getType()));
                }
                callMethodsWith(onLoads, instance);
                return instance;
            } catch (final SQLException | InstantiationException | IllegalAccessException e) {
                throw new PersistenceException(e);
            } catch (final InvocationTargetException e) {
                throw new PersistenceException(e.getTargetException());
            }
        };
    }

    public Stream<String> toNames(final ResultSet resultSet) throws SQLException {
        final var metaData = resultSet.getMetaData();
        return IntStream.rangeClosed(1, metaData.getColumnCount()).mapToObj(i -> {
            try {
                return metaData.getColumnName(i);
            } catch (final SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public void onInsert(final Object instance, final PreparedStatement statement) {
        callMethodsWith(onInserts, instance);

        int idx = 1;
        for (final var field : fields.values()) {
            doBind(instance, statement, idx++, field);
        }
    }

    public void onDelete(final Object instance, final PreparedStatement statement) {
        callMethodsWith(onDeletes, instance);

        int idx = 1;
        for (final var field : idFields) {
            doBind(instance, statement, idx++, field);
        }
    }

    public void onUpdate(final Object instance, final PreparedStatement statement) {
        callMethodsWith(onUpdates, instance);

        int idx = 1;
        for (final var field : fields.values()) {
            doBind(instance, statement, idx++, field);
        }
        for (final var field : idFields) {
            doBind(instance, statement, idx++, field);
        }
    }

    public void onFindById(final PreparedStatement stmt, final Object id) {
        if (idFields.size() == 1) {
            try {
                stmt.setObject(1, id);
            } catch (final SQLException ex) {
                throw new PersistenceException(ex);
            }
            return;
        }

        int idx = 1;
        if (Object[].class.isInstance(id)) {
            final var ids = Object[].class.cast(id);
            if (ids.length != idFields.size()) {
                throw new IllegalArgumentException("Invalid id, expected " + idFields.size() + " bindings but got " + ids.length + ": " + idFields);
            }
            for (final var field : idFields) {
                final var value = ids[idx - 1];
                try {
                    database.doBind(stmt, idx, value, field.getType());
                    idx++;
                } catch (final SQLException ex) {
                    throw new PersistenceException(ex);
                }
            }
        } else if (Collection.class.isInstance(id)) {
            final var ids = Collection.class.cast(id);
            if (ids.size() != idFields.size()) {
                throw new IllegalArgumentException("Invalid id, expected " + idFields.size() + " bindings but got " + ids.size() + ": " + idFields);
            }
            final var it = ids.iterator();
            for (final var field : idFields) {
                final var value = it.next();
                try {
                    database.doBind(stmt, idx, value, field.getType());
                    idx++;
                } catch (final SQLException ex) {
                    throw new PersistenceException(ex);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid id, ensure to pass an object array or collection");
        }
    }

    private Comparator<Map.Entry<String, ParameterHolder>> constructorOrder() {
        return Comparator.<Map.Entry<String, ParameterHolder>, Integer>comparing(e -> idFields.stream()
                        .filter(i -> i.getName().equals(e.getValue().parameter.getName()))
                        .findFirst()
                        .map(idFields::indexOf)
                        .orElse(Integer.MAX_VALUE))
                .thenComparing(Map.Entry::getKey);
    }

    private Comparator<Map.Entry<String, Field>> fieldOrder() {
        return Comparator.<Map.Entry<String, Field>, Integer>comparing(e -> idFields.contains(e.getValue()) ?
                        idFields.indexOf(e.getValue()) :
                        Integer.MAX_VALUE)
                .thenComparing(Map.Entry::getKey);
    }

    private void doBind(final Object instance, final PreparedStatement statement, final int idx, final Field field) {
        try {
            database.doBind(statement, idx, field.get(instance), field.getType());
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final SQLException ex) {
            throw new PersistenceException(ex);
        }
    }

    private void callMethodsWith(final List<Method> callback, final Object instance) {
        if (callback.isEmpty()) {
            return;
        }
        callback.forEach(m -> {
            try {
                m.invoke(instance);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new PersistenceException(e.getTargetException());
            }
        });
    }

    private Stream<Parameter> captureConstructorParameters(final Class<?> type) {
        return Stream.of(type.getConstructors())
                .max(comparing(Constructor::getParameterCount))
                .map(c -> Stream.of(c.getParameters()))
                .orElseThrow(() -> new IllegalArgumentException("No constructor for " + type));
    }

    private Stream<Field> captureFields(final Class<?> type) {
        return type == Object.class || type == null ?
                Stream.empty() :
                Stream.concat(
                                Stream.of(type.getDeclaredFields()),
                                captureFields(type.getSuperclass()))
                        .filter(it -> !Modifier.isStatic(it.getModifiers()) && !it.isSynthetic())
                        .filter(this::hasPersistentAnnotation)
                        .peek(it -> it.setAccessible(true));
    }

    private Stream<Method> captureMethods(final Class<?> type, final Class<? extends Annotation> marker) {
        return type == Object.class || type == null ?
                Stream.empty() :
                Stream.concat(
                                Stream.of(type.getDeclaredMethods()),
                                captureMethods(type.getSuperclass(), marker))
                        .filter(it -> !Modifier.isStatic(it.getModifiers()) && !it.isSynthetic() && it.isAnnotationPresent(marker))
                        .peek(it -> it.setAccessible(true));
    }

    private boolean hasPersistentAnnotation(final AnnotatedElement element) {
        return element.isAnnotationPresent(Id.class) || element.isAnnotationPresent(Column.class);
    }

    private Optional<String> type(final ColumnMetadata columnMetadata) {
        return ofNullable(columnMetadata.getAnnotation(Column.class))
                .map(Column::type)
                .filter(it -> !it.isBlank());
    }

    private String name(final Field f) {
        return name((AnnotatedElement) f)
                .or(() -> ofNullable(constructorParameters.get(f.getName()))
                        .map(p -> p.parameter)
                        .flatMap(this::name))
                .orElseGet(f::getName);
    }

    private Optional<String> name(final AnnotatedElement element) {
        return ofNullable(element.getAnnotation(Column.class))
                .map(Column::name)
                .filter(it -> !it.isBlank());
    }

    private Annotation[] mergeAnnotations(final Field field) {
        return Stream.concat(
                        Stream.of(field.getAnnotations()),
                        ofNullable(constructorParameters.get(field.getName()))
                                .map(it -> Stream.of(it.parameter.getAnnotations()))
                                .orElseGet(Stream::empty))
                .toArray(Annotation[]::new);
    }

    private static class ParameterHolder {
        private final Parameter parameter;
        private final int index;

        public final Object defaultValue;
        private final int hash;

        private ParameterHolder(final Parameter parameter, final int index) {
            this.parameter = parameter;
            this.index = index;
            this.hash = Objects.hash(parameter, index);
            this.defaultValue = findDefault(parameter.getType());
        }

        private Object findDefault(final Class<?> type) {
            if (type == short.class) {
                return (byte) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0.f;
            }
            if (type == double.class) {
                return 0.;
            }
            if (type == boolean.class) {
                return false;
            }
            return null;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ParameterHolder that = (ParameterHolder) o;
            return index == that.index && parameter.equals(that.parameter);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class CaseInsensitiveLinkedHashMap<B> extends LinkedHashMap<String, B> { // todo: full delegation instead of inheritance
        // for runtime lookup by column name only
        private final Map<String, B> lookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        @Override
        public B put(final String key, final B value) {
            lookup.put(key, value);
            return super.put(key, value);
        }

        @Override
        public B merge(final String key, final B value, final BiFunction<? super B, ? super B, ? extends B> remappingFunction) {
            lookup.merge(key, value, remappingFunction);
            return super.merge(key, value, remappingFunction);
        }

        @Override
        public B get(final Object key) {
            return lookup.get(key);
        }

        // other methods are not used (why we need to break this inheritance thing)
    }
}