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
package io.yupiik.uship.jsonrpc.doc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public final class CliSibling {
    private CliSibling() {
        // no-op
    }

    public static Reader toReader(final String arg) throws IOException {
        if ("stdin".equals(ofNullable(arg).orElse("stdin"))) {
            return new BufferedReader(new InputStreamReader(System.in)) {
                @Override
                public void close() {
                    // no-op
                }
            };
        }
        final var path = Paths.get(arg);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        return Files.newBufferedReader(path);
    }

    public static PrintStream toOutputStream(final String arg) throws IOException {
        switch (ofNullable(arg).orElse("stdout")) {
            case "stdout":
                return new PrintStream(System.out) {
                    @Override
                    public void close() {
                        flush();
                    }
                };
            case "stderr":
                return new PrintStream(System.err) {
                    @Override
                    public void close() {
                        flush();
                    }
                };
            default:
                final var path = Paths.get(arg);
                if (!Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }
                return new PrintStream(Files.newOutputStream(path));
        }
    }

    public static List<Class<?>> mapClasses(final String arg) {
        return Stream.of(arg.split(","))
                .map(String::trim)
                .filter(it -> !it.isBlank())
                .map(clazz -> {
                    try {
                        return Thread.currentThread().getContextClassLoader().loadClass(clazz);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .collect(toList());
    }
}
