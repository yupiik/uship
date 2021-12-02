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
package io.yupiik.uship.persistence.impl.operation;

import io.yupiik.uship.persistence.api.Database;
import io.yupiik.uship.persistence.api.Entity;
import io.yupiik.uship.persistence.api.PersistenceException;
import io.yupiik.uship.persistence.api.ResultSetWrapper;
import io.yupiik.uship.persistence.api.SQLFunction;
import io.yupiik.uship.persistence.api.StatementBinder;
import io.yupiik.uship.persistence.api.operation.Operation;
import io.yupiik.uship.persistence.api.operation.Statement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Operations implements InvocationHandler {
    private static final Execution FAIL = args -> {
        throw new UnsupportedOperationException("Unknown operation");
    };

    private final Database database;
    private final Map<Method, Execution> operations;

    public <M> Operations(final Database database, final Class<M> api, final ClassLoader loader) {
        this.database = database;

        final Map<String, Entity<?>> aliases = ofNullable(api.getAnnotation(Operation.class))
                .map(Operation::aliases)
                .map(a -> new HashMap<String, Entity<?>>(Stream.of(a)
                        .collect(toMap(Operation.Alias::alias, it -> database.getOrCreateEntity(it.type())))))
                .orElseGet(HashMap::new); // we can enrich it for fqn so ensure it is writable
        this.operations = Stream.of(api.getMethods())
                .filter(m -> m.isAnnotationPresent(Statement.class))
                .collect(toMap(identity(), m -> prepare(aliases, loader, m)));
    }

    private Execution prepare(final Map<String, Entity<?>> aliases, final ClassLoader loader, final Method method) {
        final var rawStmt = method.getAnnotation(Statement.class).value();

        final var placeholderParameters = rawStmt.contains("${parameters#");
        final var parameterNames = placeholderParameters ?
                Stream.of(method.getParameters()).map(Parameter::getName).collect(toList()) :
                List.of();

        final var bindings = placeholderParameters ? new ArrayList<Integer>() : List.<Integer>of();
        final var sql = new Substitutor(key -> {
            final int sep = key.indexOf('#');
            if (sep < 0) {
                throw new IllegalArgumentException("Invalid placeholder: '" + key + "', expected '<alias>#fields', '<alias>#table', 'parameters#<name>'");
            }

            final var prefix = key.substring(0, sep).strip();
            final var suffix = key.substring(sep + 1).strip();

            if ("parameters".equals(prefix)) {
                int index = parameterNames.indexOf(suffix);
                if (index < 0) {
                    throw new IllegalArgumentException("Invalid parameter binding: '" + suffix + "' in " + method);
                }
                bindings.add(index);
                return "?";
            }

            final var entity = aliases.computeIfAbsent(prefix, k -> {
                try {
                    return database.getOrCreateEntity(loader.loadClass(k));
                } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            });
            switch (suffix.toLowerCase(ROOT)) {
                case "table":
                    return entity.getTable();
                case "fields":
                    return entity.concatenateColumns(new Entity.ColumnsConcatenationRequest());
                default:
                    throw new IllegalArgumentException("Unknown attribute of entity: '" + key + "'");
            }
        }).replace(rawStmt);

        final BiConsumer<Object[], StatementBinder> binder;
        if (method.getParameterCount() == 0) {
            binder = (a, b) -> {
            };
        } else if (placeholderParameters) {
            final var types = method.getParameterTypes();
            binder = (args, b) -> bindings.forEach(i -> b.bind(types[i], args[i]));
        } else {
            final var types = method.getParameterTypes();
            binder = (args, b) -> IntStream.range(0, args.length).forEach(i -> b.bind(types[i], args[i]));
        }

        if (!sql.toLowerCase(ROOT).strip().startsWith("select")) {
            return args -> database.execute(sql, b -> binder.accept(args, b));
        }

        final Function<ResultSetWrapper, Object> outputMapper;
        final var returnType = method.getGenericReturnType();
        if (void.class == returnType) {
            outputMapper = rset -> null;
        } else if (long.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getLong(1));
        } else if (int.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getInt(1));
        } else if (boolean.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getBoolean(1));
        } else if (float.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getFloat(1));
        } else if (double.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getDouble(1));
        } else if (Number.class == returnType) {
            outputMapper = rset -> mapFirst(rset, r -> r.getObject(1));
        } else if (ParameterizedType.class.isInstance(returnType) &&
                Collection.class.isAssignableFrom(Class.class.cast(ParameterizedType.class.cast(returnType).getRawType()))) {
            outputMapper = rset -> database.mapAll(Class.class.cast(ParameterizedType.class.cast(returnType).getActualTypeArguments()[0]), rset.get());
        } else { // single line
            final var type = method.getReturnType();
            outputMapper = rset -> mapFirst(rset, r -> database.mapOne(type, r));
        }

        return args -> database.query(sql, b -> binder.accept(args, b), outputMapper);
    }

    private Object mapFirst(final ResultSetWrapper rset, final SQLFunction<ResultSet, Object> mapper) {
        if (!rset.hasNext()) {
            throw new PersistenceException("No result found");
        }
        try {
            final var res = mapper.apply(rset.get());
            if (rset.hasNext()) {
                throw new PersistenceException("Ambiguous entity fetched!");
            }
            return res;
        } catch (final SQLException sqlex) {
            throw new PersistenceException(sqlex);
        }
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            switch (method.getName()) {
                case "equals":
                    return args.length == 1 && doEquals(proxy, args[0]);
                case "hashCode":
                    return super.hashCode();
                case "toString":
                    return "Operations[" + operations + "]";
                default: {
                    try {
                        return method.invoke(this, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                }
            }
        }
        return operations.getOrDefault(method, FAIL).execute(args);
    }

    private boolean doEquals(final Object proxy, final Object arg) { // it is a reference equals for the handler
        return arg == proxy || (arg != null && Proxy.isProxyClass(arg.getClass()) && super.equals(Proxy.getInvocationHandler(arg)));
    }

    private interface Execution {
        Object execute(Object[] args);
    }

    // from commons-text
    private static class Substitutor {
        private static final char ESCAPE = '$';
        private static final char[] PREFIX = "${".toCharArray();
        private static final char[] SUFFIX = "}".toCharArray();
        private static final char[] VALUE_DELIMITER = ":-".toCharArray();

        private final Function<String, String> valueMap;

        private Substitutor(final Function<String, String> valueMap) {
            this.valueMap = valueMap;
        }

        public String replace(final String source) {
            if (source == null) {
                return null;
            }
            final StringBuilder builder = new StringBuilder(source);
            if (substitute(builder, 0, source.length(), null) <= 0) {
                return source;
            }
            return replace(builder.toString());
        }

        private int substitute(final StringBuilder buf, final int offset, final int length,
                               List<String> priorVariables) {
            final boolean top = priorVariables == null;
            boolean altered = false;
            int lengthChange = 0;
            char[] chars = buf.toString().toCharArray();
            int bufEnd = offset + length;
            int pos = offset;
            while (pos < bufEnd) {
                final int startMatchLen = isMatch(PREFIX, chars, pos, bufEnd);
                if (startMatchLen == 0) {
                    pos++;
                } else {
                    if (pos > offset && chars[pos - 1] == ESCAPE) {
                        buf.deleteCharAt(pos - 1);
                        chars = buf.toString().toCharArray();
                        lengthChange--;
                        altered = true;
                        bufEnd--;
                    } else {
                        final int startPos = pos;
                        pos += startMatchLen;
                        int endMatchLen;
                        while (pos < bufEnd) {
                            endMatchLen = isMatch(SUFFIX, chars, pos, bufEnd);
                            if (endMatchLen == 0) {
                                pos++;
                            } else {
                                String varNameExpr = new String(chars, startPos
                                        + startMatchLen, pos - startPos
                                        - startMatchLen);
                                pos += endMatchLen;
                                final int endPos = pos;

                                String varName = varNameExpr;
                                String varDefaultValue = null;

                                final char[] varNameExprChars = varNameExpr.toCharArray();
                                for (int i = 0; i < varNameExprChars.length; i++) {
                                    if (isMatch(PREFIX, varNameExprChars, i, varNameExprChars.length) != 0) {
                                        break;
                                    }
                                    final int match = isMatch(VALUE_DELIMITER, varNameExprChars, i, varNameExprChars.length);
                                    if (match != 0) {
                                        varName = varNameExpr.substring(0, i);
                                        varDefaultValue = varNameExpr.substring(i + match);
                                        break;
                                    }
                                }

                                if (priorVariables == null) {
                                    priorVariables = new ArrayList<>();
                                    priorVariables.add(new String(chars,
                                            offset, length));
                                }

                                priorVariables.add(varName);

                                final String varValue = getOrDefault(varName, varDefaultValue);
                                if (varValue != null) {
                                    final int varLen = varValue.length();
                                    buf.replace(startPos, endPos, varValue);
                                    altered = true;
                                    int change = substitute(buf, startPos, varLen, priorVariables);
                                    change = change + varLen - (endPos - startPos);
                                    pos += change;
                                    bufEnd += change;
                                    lengthChange += change;
                                    chars = buf.toString().toCharArray();
                                }

                                priorVariables.remove(priorVariables.size() - 1);
                                break;
                            }
                        }
                    }
                }
            }
            if (top) {
                return altered ? 1 : 0;
            }
            return lengthChange;
        }

        private String getOrDefault(final String varName, final String varDefaultValue) {
            final var value = valueMap.apply(varName);
            return value == null ? varDefaultValue : value;
        }

        private int isMatch(final char[] chars, final char[] buffer, int pos,
                            final int bufferEnd) {
            final int len = chars.length;
            if (pos + len > bufferEnd) {
                return 0;
            }
            for (int i = 0; i < chars.length; i++, pos++) {
                if (chars[i] != buffer[pos]) {
                    return 0;
                }
            }
            return len;
        }
    }
}
