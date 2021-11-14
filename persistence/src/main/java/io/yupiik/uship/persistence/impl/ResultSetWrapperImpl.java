package io.yupiik.uship.persistence.impl;

import io.yupiik.uship.persistence.api.PersistenceException;
import io.yupiik.uship.persistence.api.ResultSetWrapper;
import io.yupiik.uship.persistence.api.SQLFunction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class ResultSetWrapperImpl implements ResultSetWrapper {
    private final ResultSet resultSet;

    public ResultSetWrapperImpl(final ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public boolean hasNext() {
        try {
            return resultSet.next();
        } catch (final SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public <T> List<T> mapAll(final SQLFunction<ResultSet, T> mapper) {
        return stream(spliteratorUnknownSize(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                try {
                    return resultSet.next();
                } catch (final SQLException e) {
                    throw new PersistenceException(e);
                }
            }

            @Override
            public T next() {
                try {
                    return mapper.apply(resultSet);
                } catch (final SQLException e) {
                    throw new PersistenceException(e);
                }
            }
        }, IMMUTABLE), false).collect(toList());
    }

    @Override
    public <T> T map(final SQLFunction<ResultSet, T> mapper) {
        try {
            return mapper.apply(resultSet);
        } catch (final SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public ResultSet get() {
        return resultSet;
    }
}
