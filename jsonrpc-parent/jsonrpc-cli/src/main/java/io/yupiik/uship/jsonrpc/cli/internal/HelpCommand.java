/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.cli.internal;

import io.yupiik.uship.backbone.johnzon.jsonschema.Schema;
import io.yupiik.uship.backbone.johnzon.jsonschema.SchemaProcessor;
import io.yupiik.uship.jsonrpc.cli.api.JsonRpcCliExecutor;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import io.yupiik.uship.jsonrpc.core.impl.JsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.impl.Registration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

@JsonRpc
@ApplicationScoped
public class HelpCommand {
    @Inject
    private JsonRpcCliExecutor executor;

    @Inject
    private JsonRpcMethodRegistry registry;

    @Inject
    private Jsonb jsonb;

    @JsonRpcMethod(name = "help", documentation = "Show help (available commands, options).")
    public String help(@JsonRpcParam(value = "format", documentation = "Output format (TEXT, ADOC)") final HelpFormat format,
                       @JsonRpcParam(value = "command", documentation = "Filter the documentation for a single command.") final String command) {
        switch (ofNullable(format).orElse(HelpFormat.TEXT)) {
            case TEXT:
                return getErrorText(command);
            case ADOC:
                return getAdocText(command);
            default:
                throw new IllegalArgumentException("Unsupported encoding mode: " + format);
        }
    }

    private String getErrorText(final String command) {
        final var commands = registry.getHandlers().entrySet().stream()
                .filter(it -> command == null || command.equals(it.getKey()))
                .map(it -> "" +
                        "  " + it.getKey() + ":\n\n    " +
                        it.getValue().registration().documentation().replace("\n", "\n    ") + "\n\n" +
                        it.getValue().registration().parameters().stream()
                                .flatMap(this::flatten)
                                .sorted(comparing(p -> p.name))
                                .map(p -> "    --" + p.name + " (" + p.type + "): " + p.doc)
                                .collect(joining("\n")) + '\n')
                .sorted()
                .collect(joining("\n"));
        return command == null ? "Yupiik JSON-RPC Cli Help:\n\n" +
                "Commands:\n\n" +
                commands + "\n\n" +
                "Options syntax:\n\n  " +
                getOptionsSyntax().replace("\n", "\n  ") + "\n\n" +
                "Global options:\n\n" +
                "  All commands support the following additional options:\n\n  " +
                getBuiltInOptions().replace("\n", "\n  ") + '\n' : commands.trim();
    }

    private String getAdocText(final String command) {
        final var commands = registry.getHandlers().entrySet().stream()
                .filter(it -> command == null || command.equals(it.getKey()))
                .map(it -> "" +
                        "=== " + it.getKey() + "\n\n" +
                        it.getValue().registration().documentation() + "\n\n" +
                        "==== Options\n\n" +
                        it.getValue().registration().parameters().stream()
                                .flatMap(this::flatten)
                                .sorted(comparing(p -> p.name))
                                .map(p -> "`--" + p.name + "` (`" + p.type + "`):: " + p.doc)
                                .collect(joining("\n")) + '\n')
                .sorted()
                .collect(joining("\n"));
        return command == null ? "= Yupiik JSON-RPC Cli Help\n\n" +
                "== Commands\n\n" +
                commands + "\n\n" +
                "== Options syntax\n\n" +
                getOptionsSyntax() + "\n\n" +
                "== Global options\n\n" +
                "All commands support the following additional options:\n\n- " +
                getBuiltInOptions().replace("\n", "\n- ") : commands.trim();
    }

    private String getBuiltInOptions() {
        return "" +
                "--cli-env: takes a properties file as parameter containing options merged with command line ones. It enables to save commands to reexecute them easily.\n" +
                "--cli-response-dump: take a properties file path as value and triggers a dump of a successful command response as properties (useful to chain commands more easily for example in a script).\n" +
                "--cli-response-dump-delete-on-exit: if set to true, the dump deleted - if created with success - when the CLI exits.\n" +
                "--cli-silent: if set to true, the command will not output sucess response (useful in batches).";
    }

    private Stream<Param> flatten(final Registration.Parameter parameter) {
        final Type type = parameter.type();
        if (isPrimitive(type)) {
            return Stream.of(new Param(
                    parameter.name(), toString(type.getTypeName()),
                    parameter.documentation(), parameter.required()));
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class) {
                final Class<?> rawClass = (Class<?>) pt.getRawType();
                if (Collection.class.isAssignableFrom(rawClass)) {
                    if (isPrimitive(pt.getActualTypeArguments()[0])) {
                        return Stream.of(new Param(
                                parameter.name() + "-<index>", toString(type.getTypeName()),
                                parameter.documentation(), parameter.required()));
                    }
                    return Stream.concat(
                            Stream.of(new Param(
                                    parameter.name() + "-<index>",
                                    toString(parameter.type().getTypeName()) + " - JSON array",
                                    parameter.documentation(), parameter.required())),
                            flattenObject(parameter.name(), parameter.type()));
                } else if (Map.class.isAssignableFrom(rawClass)) {
                    if (isPrimitive(pt.getActualTypeArguments()[0]) && isPrimitive(pt.getActualTypeArguments()[1])) {
                        return Stream.concat(
                                Stream.of(new Param(
                                        parameter.name(),
                                        toString(parameter.type().getTypeName()) + " - JSON object",
                                        parameter.documentation(), parameter.required())),
                                Stream.of(
                                        new Param(
                                                parameter.name() + "-<index>-key", toString(type.getTypeName()),
                                                "Key of that indexed map entry.", parameter.required()),
                                        new Param(
                                                parameter.name() + "-<index>-value", toString(type.getTypeName()),
                                                "Value of that indexed map entry.", parameter.required())));
                    }
                    // else not supported
                }
            }
        } else if (type instanceof Class) {
            return Stream.concat(
                    Stream.of(new Param(
                            parameter.name(), toString(parameter.type().getTypeName()) + " - JSON object",
                            parameter.documentation(), parameter.required())),
                    flattenObject(parameter.name(), parameter.type()));
        }
        throw new IllegalArgumentException("Unsupported command parameter: " + type);
    }

    private Stream<Param> flattenObject(final String prefix, final Type type) {
        try (final SchemaProcessor processor = new SchemaProcessor(true, false, s -> jsonb.fromJson(s, Schema.class))) {
            return flatten(prefix, processor.mapSchemaFromClass(type));
        }
    }

    private Stream<Param> flatten(final String prefix, final Schema schema) {
        switch (schema.getType()) {
            case string:
            case integer:
            case bool:
            case number:
                return Stream.of(new Param(
                        prefix, schema.getType().name(), requireNonNull(schema.getDescription(), "no description"), false));
            case object:
                if (schema.getProperties().isEmpty()) { // assume map<string, string>
                    return Stream.of(
                            new Param(prefix + "-<index>-key", "String", "Map key.", true),
                            new Param(prefix + "-<index>-value", "String", "Map value.", true));
                }
                return schema.getProperties().entrySet().stream()
                        .flatMap(e -> {
                            switch (e.getValue().getType()) {
                                case object:
                                    return flatten(prefix + '-' + e.getKey(), e.getValue());
                                case array:
                                    return flatten(prefix + "-<index>", e.getValue());
                                default:
                                    return Stream.of(new Param(
                                            prefix + '-' + e.getKey(),
                                            e.getValue().getTitle() == null ? schema.getType().name() : toString(e.getValue().getTitle()),
                                            e.getValue().getDescription(),
                                            false));
                            }
                        });
            case array:
                return flatten(prefix + "-<index>", schema.getItems());
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + schema);
        }
    }

    private boolean isPrimitive(final Type type) {
        return type == String.class || (type instanceof Class && ((Class<?>) type).isEnum()) ||
                type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Boolean.class || type == boolean.class;
    }

    private String getOptionsSyntax() {
        return "- List can be specified expanding the option name with an index, for example '--my-list' will specify values using '--my-list-0', '--my-list-1', etc...\n" +
                "- Objects can be specified expanding the option name with suboption names, for example an object '--my-object' with a name attribute will specify the name with '--my-object-name'\n" +
                "- Maps follow the list pattern suffixed with '-key' and '-value', for instance to set [a:b] for the option 'my-map', you will set '--my-map-0-key a --my-map-0-value b\n" +
                "- A file content can be injected in an option prefixing it with '@', for example '--my-json @content.json', if you really want to pass the value '@content.json' you need to escape the '@' with another '@': '@@content.json' will inject '@content.json' value.";
    }

    private String toString(final String typeName) {
        final int generic = typeName.indexOf("<");
        if (generic > 0) {
            return toString(typeName.substring(0, generic)) +
                    '<' +
                    Stream.of(typeName.substring(generic + 1, typeName.lastIndexOf('>')))
                            .map(this::toString)
                            .collect(joining(", ")) +
                    '>';
        }
        int start = typeName.lastIndexOf('$');
        if (start < 0) {
            start = typeName.lastIndexOf('.');
        }
        return start < 0 ? typeName : typeName.substring(start + 1);
    }

    public enum HelpFormat {
        // @Doc(description = "Plain text")
        TEXT,

        // @Doc(description = "Asciidoc")
        ADOC
    }

    private static class Param {
        private final String name;
        private final String type;
        private final String doc;
        private final boolean required;

        private Param(final String name, final String type, final String doc, final boolean required) {
            this.name = name;
            this.type = type;
            this.doc = doc;
            this.required = required;
        }
    }
}
