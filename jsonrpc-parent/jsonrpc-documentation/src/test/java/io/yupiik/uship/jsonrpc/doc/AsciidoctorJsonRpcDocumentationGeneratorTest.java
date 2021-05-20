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

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsciidoctorJsonRpcDocumentationGeneratorTest {
    @Test
    void generate() {
        final var out = new ByteArrayOutputStream();
        try (final var stream = new PrintStream(out)) {
            new AsciidoctorJsonRpcDocumentationGenerator(
                    "Test Title",
                    List.of(Endpoints.class),
                    stream
            ).run();
        }
        assertEquals("" +
                "= Test Title\n" +
                "\n" +
                "== JSON-RPC Methods\n" +
                "\n" +
                "=== test1\n" +
                "\n" +
                "==== Parameters\n" +
                "\n" +
                "[options=\"header\"]\n" +
                "|===\n" +
                "|Name|Position|Type|Required|Documentation\n" +
                "|in|0|String|false|-\n" +
                "|===\n" +
                "\n" +
                "==== Result type\n" +
                "\n" +
                "`io.yupiik.uship.jsonrpc.doc.AsciidoctorJsonRpcDocumentationGeneratorTest$Foo`\n" +
                "\n" +
                "\n" +
                "=== test2\n" +
                "\n" +
                "==== Parameters\n" +
                "\n" +
                "[options=\"header\"]\n" +
                "|===\n" +
                "|Name|Position|Type|Required|Documentation\n" +
                "|in|0|String|false|-\n" +
                "|===\n" +
                "\n" +
                "==== Result type\n" +
                "\n" +
                "`String`\n" +
                "\n" +
                "\n" +
                "== Model Schemas\n" +
                "\n" +
                "[#io_yupiik_uship_jsonrpc_doc_AsciidoctorJsonRpcDocumentationGeneratorTest_Foo]\n" +
                "=== io.yupiik.uship.jsonrpc.doc.AsciidoctorJsonRpcDocumentationGeneratorTest$Foo\n" +
                "\n" +
                "[cols=\"2,2m,1,5\", options=\"header\"]\n" +
                ".io.yupiik.uship.jsonrpc.doc.AsciidoctorJsonRpcDocumentationGeneratorTest$Foo\n" +
                "|===\n" +
                "|Name|JSON Name|Type|Description\n" +
                "|name|name|string|-\n" +
                "|===\n" +
                "\n" +
                "", out.toString(StandardCharsets.UTF_8));
    }

    public static class Foo {
        private String name;

        public Foo() {
            // no-op
        }

        public Foo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @JsonRpc
    public static class Endpoints {
        @JsonRpcMethod(name = "test1")
        public Foo test1(@JsonRpcParam final String in) {
            return new Foo(new StringBuilder(in).reverse().toString());
        }

        @JsonRpcMethod(name = "test2")
        public String test2(@JsonRpcParam final String in) {
            return in;
        }
    }
}
