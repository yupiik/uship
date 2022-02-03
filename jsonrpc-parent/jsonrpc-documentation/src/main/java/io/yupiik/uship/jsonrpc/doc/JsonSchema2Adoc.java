/*
 * Copyright (c) 2021, 2022 - Yupiik SAS - https://www.yupiik.com
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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class JsonSchema2Adoc implements Supplier<StringBuilder> {
    private final String levelPrefix;
    private final Schema schema;
    private final Predicate<Schema> shouldIgnore;

    public JsonSchema2Adoc(final String levelPrefix, final Schema schema, final Predicate<Schema> shouldIgnore) {
        this.levelPrefix = levelPrefix;
        this.schema = schema;
        this.shouldIgnore = shouldIgnore;
    }

    public void prepare(final Schema in) {
        final Schema schema = ofNullable(in).orElse(this.schema);
        switch (schema.getType()) {
            case string:
            case integer:
            case number:
            case bool:
                break;
            case object:
                final Map<String, Schema> properties = schema.getProperties();
                if (properties != null && !properties.isEmpty()) {
                    properties.values().forEach(this::prepare);
                }
                break;
            case array:
                if (schema.getItems() == null) {
                    throw new IllegalArgumentException("No items in array schema:\n" + schema);
                }
                if (schema.getItems().getTitle() == null) {
                    schema.getItems().setTitle(schema.getTitle());
                }
                if (schema.getItems().getDescription() == null) {
                    schema.getItems().setDescription(schema.getDescription());
                }
                if (schema.getTitle() == null) {
                    schema.setTitle(schema.getItems().getTitle());
                }
                if (schema.getDescription() == null) {
                    schema.setDescription(schema.getItems().getDescription());
                }
                if (schema.getItems().getType() == Schema.SchemaType.object) {
                    prepare(schema.getItems());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown schema type: " + schema.getType());
        }
    }

    @Override
    public StringBuilder get() {
        if (shouldIgnore.test(schema)) {
            return new StringBuilder();
        }

        final String title = requireNonNull(schema.getTitle(), "missing title");
        final String description = requireNonNull(schema.getDescription(), "missing description");

        final StringBuilder main = new StringBuilder();
        main.append(levelPrefix).append(' ').append(title).append("\n\n");
        if (!description.isEmpty()) {
            main.append(description).append("\n\n");
        }

        switch (schema.getType()) {
            case string:
            case integer:
            case number:
            case bool:
                break;
            case object:
                final Map<String, Schema> properties = schema.getProperties();
                if (properties != null && !properties.isEmpty()) {
                    final Map<String, Schema> primitives = properties.entrySet().stream()
                            .filter(it -> it.getValue().getType() != Schema.SchemaType.array && it.getValue().getType() != Schema.SchemaType.object)
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
                    final Map<String, Schema> nestedObjects = properties.entrySet().stream()
                            .filter(it -> it.getValue().getType() == Schema.SchemaType.object)
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
                    final Map<String, Schema> nestedArrays = properties.entrySet().stream()
                            .filter(it -> it.getValue().getType() == Schema.SchemaType.array)
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

                    main.append("[cols=\"2,2m,1,5\", options=\"header\"]\n.")
                            .append(title)
                            .append("\n|===\n")
                            .append("|Name|JSON Name|Type|Description\n");
                    primitives.forEach((name, schema) -> main.append('|').append(name)
                            .append('|').append(name)
                            .append('|').append(schema.getType())
                            .append('|').append(toDescription(schema))
                            .append(extractPotentialValues(schema))
                            .append(extractDefaultValue(schema))
                            .append('\n'));
                    nestedArrays.entrySet().stream()
                            .filter(e -> e.getValue().getItems().getType() != Schema.SchemaType.object)
                            .forEach(e -> main.append('|').append(e.getKey())
                                    .append('|').append(e.getKey())
                                    .append('|').append("array of ").append(e.getValue().getItems().getType())
                                    .append('|').append(toDescription(e.getValue())).append('\n'));
                    final List<Map.Entry<String, Schema>> validArrayKeys = nestedArrays.entrySet().stream()
                            .filter(e -> e.getValue().getItems().getType() == Schema.SchemaType.object)
                            .filter(e -> {
                                final boolean object = e.getValue().getItems().getType() != Schema.SchemaType.object;
                                final boolean ignoreObjectBlock = e.getValue().getItems().getRef() != null;
                                final String anchor = toAnchor(object ? e.getValue().getItems() : e.getValue());
                                main.append("|").append(anchor == null ? "" : "<<").append(anchor).append(anchor == null ? "" : ">>").append("|")
                                        .append(e.getKey()).append('|')
                                        .append("array of ").append(e.getValue().getItems().getType()).append('|')
                                        .append(e.getValue().getDescription() == null ? toDescription(e.getValue().getItems()) : toDescription(e.getValue())).append('\n');
                                return !ignoreObjectBlock;
                            })
                            .collect(toList());
                    final List<String> validObjectKeys = nestedObjects.entrySet().stream()
                            .filter(e -> {
                                final boolean ignoreObjectBlock = e.getValue().getRef() != null;
                                main.append("|");

                                final String anchor = toAnchor(e.getValue());
                                if (anchor != null) {
                                    main.append("<<").append(anchor).append(">>");
                                } else {
                                    main.append(e.getKey());
                                }
                                main.append('|').append(e.getKey()).append('|')
                                        .append(e.getValue().getType()).append('|')
                                        .append(toDescription(e.getValue())).append('\n');
                                return !ignoreObjectBlock;
                            })
                            .map(Map.Entry::getKey)
                            .collect(toList());
                    main.append("|===\n");

                    if (!validObjectKeys.isEmpty()) {
                        main.append("\n");
                        nestedObjects.entrySet().stream()
                                .filter(e -> validObjectKeys.contains(e.getKey()))
                                .forEach(e -> {
                                    final String anchorBase = toAnchor(e.getValue());
                                    final StringBuilder content = new JsonSchema2Adoc(getNextLevelPrefix(), e.getValue(), shouldIgnore).get();
                                    if (anchorBase != null && content.length() > 0) {
                                        main.append("[#").append(anchorBase).append("]\n").append(content).append("\n\n");
                                    }
                                });
                    }
                    if (!validArrayKeys.isEmpty()) {
                        main.append("\n");
                        nestedArrays.entrySet().stream()
                                .filter(e -> e.getValue().getItems().getType() == Schema.SchemaType.object) // primitives are already handled and we don't have arrays of arrays
                                .forEach(e -> {
                                    final Schema items = e.getValue().getItems();
                                    final String anchorBase = toAnchor(items);
                                    final StringBuilder content = new JsonSchema2Adoc(getNextLevelPrefix(), items, shouldIgnore).get();
                                    if (anchorBase != null && content.length() > 0) {
                                        main.append("[#").append(anchorBase).append("]\n").append(content).append("\n\n");
                                    }
                                });
                    }
                }
                break;
            case array:
                throw new IllegalArgumentException("Arrays shouldn't be root configuration and are handled in previous cases. You shouldn't be able to end up there.");
            default:
                throw new IllegalArgumentException("Unknown schema type: " + schema.getType());
        }

        return main;
    }

    private String extractDefaultValue(final Schema schema) {
        return schema.getDefaultValue() != null ? " Default Value: `" +
                String.valueOf(schema.getDefaultValue()).replace("|", "\\|") + "`." : "";
    }

    private String extractPotentialValues(final Schema schema) {
        return schema.getEnumeration() != null && !schema.getEnumeration().isEmpty() ? toEnumValues(schema) : "";
    }

    private String toEnumValues(final Schema schema) {
        final Class<? extends Enum<?>> enumClass = tryToLoadEnum(schema.getId());
        return " Potential values: " + String.join(",", schema.getEnumeration().stream()
                .map(it -> "`" + it + "`" + findCommentForEnumValue(enumClass, String.valueOf(it)))
                .collect(joining(","))) + '.';
    }

    private String toDescription(final Schema schema) {
        return schema.getDescription().isEmpty() ? "-" : schema.getDescription().replace("|", "\\|");
    }

    private String findCommentForEnumValue(final Class<? extends Enum<?>> enumClass, final String value) {
        /* todo
        try {
            return ofNullable(enumClass.getField(value).getAnnotation(Doc.class))
                    .map(Doc::description)
                    .filter(it -> !it.isBlank()) // enables to skip this suffix when doc is very explicit by the value already
                    .map(d -> " (" + d + ")")
                    .orElse("");
        } catch (final Exception e) {
            Logger.getLogger(getClass().getName()).warning(() -> "No @Doc on " + enumClass.getName() + '.' + value);
            return "";
        }
         */
        return "";
    }

    private Class<? extends Enum<?>> tryToLoadEnum(final String id) {
        final StringBuilder builder = new StringBuilder();
        boolean seenClass = false;
        for (final String s : id.split("_")) {
            if (builder.length() > 0) {
                if (seenClass) {
                    builder.append('$');
                } else {
                    builder.append('.');
                    seenClass = Character.isUpperCase(s.charAt(0));
                }
            }
            builder.append(s);
        }
        final String fqn = builder.toString();
        try {
            return (Class<? extends Enum<?>>) Thread.currentThread().getContextClassLoader().loadClass(fqn);
        } catch (final Exception e) {
            Logger.getLogger(getClass().getName()).warning(() -> "Can't load '" + fqn + "' (" + id + ")");
            return null;
        }
    }

    private String getNextLevelPrefix() {
        return levelPrefix.length() >= 2 ? levelPrefix : (levelPrefix + "=");
    }

    private String toAnchor(final Schema schema) {
        final String ref;
        if (schema.getType() == Schema.SchemaType.array) {
            ref = ofNullable(schema.getItems().getId()).orElseGet(() -> schema.getItems().getRef());
        } else {
            ref = ofNullable(schema.getId()).orElseGet(schema::getRef);
        }
        if (ref == null) {
            return null;
        }
        return ref.substring(ref.lastIndexOf('/') + 1).replaceFirst("#/", "").replace('/', '_');
    }
}
