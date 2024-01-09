/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonSchema2AdocTest {
    @Test
    void generate() {
        final var name = new Schema();
        name.setType(Schema.SchemaType.string);
        name.setTitle("Name");
        name.setDescription("the name");

        final var schema = new Schema();
        schema.setType(Schema.SchemaType.object);
        schema.setTitle("User");
        schema.setDescription("the user");
        schema.setProperties(Map.of("name", name));
        schema.setRequired(List.of("name"));

        assertEquals("" +
                        "= User\n" +
                        "\n" +
                        "the user\n" +
                        "\n" +
                        "[cols=\"2,2m,1,5\", options=\"header\"]\n" +
                        ".User\n" +
                        "|===\n" +
                        "|Name|JSON Name|Type|Description\n" +
                        "|name|name|string|the name\n" +
                        "|===\n" +
                        "",
                new JsonSchema2Adoc("=", schema, i -> false).get().toString());
    }
}
