package io.yupiik.uship.jsonrpc.doc;

import java.io.IOException;
import java.io.PrintStream;
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
