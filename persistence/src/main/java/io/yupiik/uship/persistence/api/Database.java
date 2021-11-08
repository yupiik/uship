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
package io.yupiik.uship.persistence.api;

import io.yupiik.uship.persistence.api.bootstrap.Configuration;
import io.yupiik.uship.persistence.impl.DatabaseImpl;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Database operation repository.
 * <p>
 * IMPORTANT: there is no transaction management there, if you need one, ensure your datasource does
 * or is set up to run in a transactional context (autoCommit setup in particular).
 */
public interface Database {
    <T> T insert(T instance);

    <T> T update(T instance);

    <T> T delete(T instance);

    /**
     * @param type entity type.
     * @param id   the identifier of the entity or an array with all identifiers if they are multiple (sorted with {@link Id#order()}).
     * @param <T>  entity type.
     * @return the entity to find or null of none is found.
     */
    <T> T findById(Class<T> type, Object id);

    <T> List<T> query(Class<T> type, String sql, Consumer<StatementBinder> binder);

    int[] batch(String sql, Iterator<Consumer<StatementBinder>> binders);

    /**
     * @param type      entity type.
     * @param resultSet resultset positionned at the row to map (next() already called).
     * @param <T>       entity type.
     * @return the mapped entity.
     */
    <T> T mapOne(Class<T> type, ResultSet resultSet);

    <T> List<T> mapAll(Class<T> type, ResultSet resultSet);

    <T> Entity<T> getOrCreateEntity(Class<T> type);

    static Database of(final Configuration configuration) {
        return new DatabaseImpl(configuration);
    }
}
