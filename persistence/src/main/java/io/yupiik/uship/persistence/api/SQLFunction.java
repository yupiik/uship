package io.yupiik.uship.persistence.api;

import java.sql.SQLException;

public interface SQLFunction<A, T> {
    T apply(A input) throws SQLException;
}
