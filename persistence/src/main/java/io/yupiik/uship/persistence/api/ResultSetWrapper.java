package io.yupiik.uship.persistence.api;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

public interface ResultSetWrapper extends Supplier<ResultSet> {
    boolean hasNext();

    <T> List<T> mapAll(SQLFunction<ResultSet, T> mapper);

    <T> T map(SQLFunction<ResultSet, T> mapper);
}
