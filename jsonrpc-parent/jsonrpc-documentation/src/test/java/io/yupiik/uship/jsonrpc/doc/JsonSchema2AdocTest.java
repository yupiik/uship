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
