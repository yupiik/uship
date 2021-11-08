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
package io.yupiik.uship.persistence.impl.query;

import io.yupiik.uship.persistence.api.PersistenceException;
import io.yupiik.uship.persistence.api.StatementBinder;
import io.yupiik.uship.persistence.impl.DatabaseImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatementBinderImpl implements StatementBinder, AutoCloseable {
    private final DatabaseImpl database;
    private final PreparedStatement preparedStatement;
    private int index = 1;

    public StatementBinderImpl(final DatabaseImpl database, final String sql, final Connection connection) throws SQLException {
        this.database = database;
        this.preparedStatement = connection.prepareStatement(sql);
    }

    @Override
    public StatementBinderImpl bind(final Class<?> type, final Object instance) {
        try {
            database.doBind(preparedStatement, index++, instance, type);
        } catch (final SQLException e) {
            throw new PersistenceException(e);
        }
        return this;
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public void reset() {
        index = 1;
    }

    @Override
    public void close() throws SQLException {
        preparedStatement.close();
    }
}
