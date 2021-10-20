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
package io.yupiik.uship.jsonrpc.doc;

import io.yupiik.uship.backbone.johnzon.jsonschema.Schema;
import io.yupiik.uship.backbone.johnzon.jsonschema.SchemaProcessor;
import io.yupiik.uship.backbone.reflect.Reflections;
import io.yupiik.uship.jsonrpc.core.impl.Registration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public class AsciidoctorJsonRpcDocumentationGenerator extends BaseJsonRpcDocumentationGenerator {
    private final String title;
    private Collection<Class<?>> schemas;

    public AsciidoctorJsonRpcDocumentationGenerator(final String title,
                                                    final Collection<Class<?>> endpoints,
                                                    final PrintStream output) {
        super(endpoints, output);
        this.title = title;
    }

    @Override
    protected void doRun(final Stream<Registration> forRegistrations, final PrintStream output) {
        if (title != null && !title.isEmpty()) {
            output.println("= " + title + '\n');
        }
        schemas = new HashSet<>();
        try {
            output.println("== JSON-RPC Methods\n");
            super.doRun(forRegistrations, output);
            if (!schemas.isEmpty()) {
                final var cache = new SchemaProcessor.InMemoryCache();
                try (final var schemaProcessor = new SchemaProcessor(true, true)) {
                    schemas.stream()
                            .sorted(comparing(Class::getName))
                            .forEach(c -> schemaProcessor.mapSchemaFromClass(c, cache));
                }
                final var visited = new HashSet<Schema>();
                if (!cache.getSchemas().isEmpty()) {
                    output.println("== Model Schemas\n");
                }
                cache.getSchemas().entrySet().stream()
                        .sorted(comparing(s -> s.getKey().getName())).forEach(e -> {
                            final Schema schema = e.getValue();
                            if (schema.getTitle() == null) {
                                schema.setTitle(e.getKey().getName());
                            }
                            if (schema.getDescription() == null) {
                                schema.setDescription("");
                            }
                            final var jsonSchema2Adoc = new JsonSchema2Adoc(
                                    "===", schema,
                                    s -> s.getRef() != null || !visited.add(s)) {
                                @Override
                                public void prepare(final Schema in) {
                                    if (in == null) {
                                        super.prepare(in);
                                        return;
                                    }
                                    if (in.getTitle() == null) { // quite unlikely
                                        in.setTitle("Model");
                                    }
                                    if (in.getDescription() == null) {
                                        in.setDescription("");
                                    }
                                    super.prepare(in);
                                }
                            };
                            jsonSchema2Adoc.prepare(null);
                            final String content = jsonSchema2Adoc.get().toString().trim();
                            if (!content.isEmpty()) {
                                output.println("[#" + schema.getId().replaceFirst("#/definitions/", "").replace('.', '_') + ']');
                                output.println(content);
                                output.println();
                            }
                        });
            }
        } finally {
            schemas.clear();
            schemas = null;
        }
    }

    @Override
    protected String asString(final Type type) {
        if (Class.class.isInstance(type)) {
            final Class<?> clazz = Class.class.cast(type);
            if (!clazz.isPrimitive() && !clazz.getName().startsWith("java.")) {
                schemas.add(clazz);
            }
        } else if (ParameterizedType.class.isInstance(type)) { // ensure schema can be registered
            Stream.of(ParameterizedType.class.cast(type).getActualTypeArguments()).forEach(this::asString);
        }
        final var string = super.asString(type);
        switch (string) {
            case "?":
                return "Any";
            case "javax.json.JsonStructure":
                return "JSON structure (array or object)";
            default:
                return string;
        }
    }

    @Override
    protected String toString(final Registration registration) {
        return "=== " + registration.jsonRpcMethod() + "\n\n" +
                ofNullable(registration.documentation())
                        .filter(it -> !it.isEmpty())
                        .map(doc -> doc + "\n\n")
                        .orElse("") +
                (registration.parameters().stream().noneMatch(it -> it.type() != HttpServletRequest.class && it.type() != HttpServletResponse.class) ?
                        "" :
                        ("==== Parameters\n\n[options=\"header\"]\n|===\n|Name|Position|Type|Required|Documentation\n" +
                                registration.parameters().stream()
                                        .filter(it -> it.type() != HttpServletRequest.class && it.type() != HttpServletResponse.class)
                                        .map(p -> '|' + p.name() + '|' + p.position() + '|' + asString(registration.clazz(), p.type()) + '|' + p.required() +
                                                '|' + ofNullable(p.documentation()).filter(it -> !it.isBlank()).orElse("-"))
                                        .collect(joining("\n")) + "\n|===\n\n")) +
                "==== Result type\n\n`" + (isVoid(registration.returnedType()) ? "None" : asString(registration.clazz(), registration.returnedType())) + "`\n\n";
    }

    private String asString(final Class<?> clazz, final Type returnedType) {
        if (ParameterizedType.class.isInstance(returnedType)) {
            return asString(Reflections.resolveType(returnedType, clazz));
        }
        return asString(Reflections.extractRealType(clazz, returnedType));
    }

    private boolean isVoid(final Type returnedType) {
        return returnedType == Void.class || returnedType == void.class;
    }

    public static void main(final String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: java -cp ... " +
                    AsciidoctorJsonRpcDocumentationGenerator.class.getName() + " <title> <jsonrpcclasses> <output>");
        }
        try (final var output = CliSibling.toOutputStream(args[2])) {
            new AsciidoctorJsonRpcDocumentationGenerator(args[0], CliSibling.mapClasses(args[1]), output).run();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
