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
package io.yupiik.uship.backbone.johnzon.jsonschema;

import io.yupiik.uship.backbone.johnzon.jsonschema.api.JsonSchema;
import io.yupiik.uship.backbone.johnzon.jsonschema.api.JsonSchemaMetadata;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchemaProcessorTest {
    @Test
    void injectedSchema() throws Exception {
        try (final var processor = new SchemaProcessor();
             final var jsonb = JsonbBuilder.create(new JsonbConfig()
                     .withFormatting(true)
                     .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final var schema = processor.mapSchemaFromClass(MyProvidedType.class);
            assertEquals("" +
                    "{\n" +
                    "  \"properties\":{\n" +
                    "    \"foo\":{\n" +
                    "      \"type\":\"string\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}", jsonb.toJson(schema));
        }
    }

    @Test
    void injectedFieldSchemaMeta() throws Exception {
        try (final var processor = new SchemaProcessor();
             final var jsonb = JsonbBuilder.create(new JsonbConfig()
                     .withFormatting(true)
                     .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final var schema = processor.mapSchemaFromClass(MyProvidedFieldType.class);
            assertEquals("" +
                    "{\n" +
                    "  \"$id\":\"io_yupiik_uship_backbone_johnzon_jsonschema_SchemaProcessorTest_MyProvidedFieldType\",\n" +
                    "  \"properties\":{\n" +
                    "    \"foo\":{\n" +
                    "      \"description\":\"A test desc.\",\n" +
                    "      \"title\":\"Test\",\n" +
                    "      \"type\":\"string\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"type\":\"object\"\n" +
                    "}", jsonb.toJson(schema));
        }
    }

    @Test // current impl does not care of records or not but just to ensure model is still right
    void record() throws Exception {
        try (final var processor = new SchemaProcessor();
             final var jsonb = JsonbBuilder.create(new JsonbConfig()
                     .withFormatting(true)
                     .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final var schema = processor.mapSchemaFromClass(MyRecord.class);
            assertEquals("" +
                    "{\n" +
                    "  \"$id\":\"io_yupiik_uship_backbone_johnzon_jsonschema_SchemaProcessorTest_MyRecord\",\n" +
                    "  \"properties\":{\n" +
                    "    \"name\":{\n" +
                    "      \"title\":\"The name\",\n" +
                    "      \"type\":\"string\"\n" +
                    "    },\n" +
                    "    \"age\":{\n" +
                    "      \"type\":\"integer\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\":[\n" +
                    "    \"name\",\n" +
                    "    \"age\"\n" +
                    "  ],\n" +
                    "  \"type\":\"object\"\n" +
                    "}", jsonb.toJson(schema));
        }
    }

    public static class MyProvidedFieldType {
        @JsonSchemaMetadata(title = "Test", description = "A test desc.")
        public String foo;
    }

    @JsonSchema("{\"properties\":{\"foo\":{\"type\":\"string\"}}}")
    public static class MyProvidedType {
    }

    // @JohnzonRecord
    public static class MyRecord {
        @JsonSchemaMetadata(title = "The name")
        private final String name;
        private final int age;

        public MyRecord(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String name() {
            return name;
        }

        public int age() {
            return age;
        }
    }
}
