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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostmanCollectionGeneratorTest {
    @Test
    void generate(@TempDir final Path dir) throws IOException {
        final var in = dir.resolve("openrpc.json");
        Files.writeString(in, "{\n" +
                "  \"components\": {\n" +
                "    \"errors\": {},\n" +
                "    \"links\": {},\n" +
                "    \"schemas\": {\n" +
                "      \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\": {\n" +
                "        \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\",\n" +
                "        \"properties\": {\n" +
                "          \"name\": {\n" +
                "            \"type\": \"string\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"type\": \"object\"\n" +
                "      }" +
                "    }\n" +
                "  },\n" +
                "  \"info\": {\n" +
                "    \"title\": \"JSON-RPC\",\n" +
                "    \"version\": \"1.0\"\n" +
                "  },\n" +
                "  \"methods\": [\n" +
                "    {\n" +
                "      \"description\": \"\",\n" +
                "      \"errors\": [],\n" +
                "      \"externalDocs\": [],\n" +
                "      \"links\": [],\n" +
                "      \"name\": \"test4\",\n" +
                "      \"paramStructure\": \"either\",\n" +
                "      \"params\": [],\n" +
                "      \"result\": {\n" +
                "        \"name\": \"test4__result\",\n" +
                "        \"schema\": {\n" +
                "          \"properties\": {},\n" +
                "          \"type\": \"object\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"summary\": \"\",\n" +
                "      \"tags\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"description\": \"\",\n" +
                "      \"errors\": [],\n" +
                "      \"externalDocs\": [],\n" +
                "      \"links\": [],\n" +
                "      \"name\": \"test1\",\n" +
                "      \"paramStructure\": \"either\",\n" +
                "      \"params\": [\n" +
                "        {\n" +
                "          \"description\": \"\",\n" +
                "          \"name\": \"in\",\n" +
                "          \"required\": false,\n" +
                "          \"schema\": {\n" +
                "            \"type\": \"string\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"result\": {\n" +
                "        \"name\": \"test1__result\",\n" +
                "        \"schema\": {\n" +
                "          \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\",\n" +
                "          \"properties\": {\n" +
                "            \"name\": {\n" +
                "              \"type\": \"string\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"type\": \"object\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"summary\": \"\",\n" +
                "      \"tags\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"openrpc\": \"1.2.4\",\n" +
                "  \"servers\": [\n" +
                "    {\n" +
                "      \"name\": \"api\",\n" +
                "      \"summary\": \"JSON-RPC API\",\n" +
                "      \"url\": \"http://localhost:8080/jsonrpc\",\n" +
                "      \"variables\": {}\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        final var out = dir.resolve("out.json");
        PostmanCollectionGenerator.main(in.toString(), out.toString());
        assertEquals("" +
                "{\n" +
                "  \"$schema\":\"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\",\n" +
                "  \"info\":{\n" +
                "    \"name\":\"JSON-RPC\",\n" +
                "    \"schema\":\"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\",\n" +
                "    \"version\":\"1.0\"\n" +
                "  },\n" +
                "  \"items\":[\n" +
                "    {\n" +
                "      \"name\":\"test4\",\n" +
                "      \"request\":{\n" +
                "        \"body\":{\n" +
                "          \"mode\":\"raw\",\n" +
                "          \"raw\":\"{\\n  \\\"jsonrpc\\\":\\\"2.0\\\",\\n  \\\"method\\\":\\\"test4\\\"\\n}\"\n" +
                "        },\n" +
                "        \"header\":[\n" +
                "          {\n" +
                "            \"key\":\"Accept\",\n" +
                "            \"type\":\"text\",\n" +
                "            \"value\":\"application/json\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"key\":\"Content-Type\",\n" +
                "            \"type\":\"text\",\n" +
                "            \"value\":\"application/json\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"method\":\"POST\",\n" +
                "        \"url\":\"{{JSON_RPC_ENDPOINT}}\"\n" +
                "      },\n" +
                "      \"response\":[\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\":\"test1\",\n" +
                "      \"request\":{\n" +
                "        \"body\":{\n" +
                "          \"mode\":\"raw\",\n" +
                "          \"raw\":\"{\\n  \\\"jsonrpc\\\":\\\"2.0\\\",\\n  \\\"method\\\":\\\"test1\\\",\\n  \\\"params\\\":{\\n    \\\"in\\\":\\\"string\\\"\\n  }\\n}\"\n" +
                "        },\n" +
                "        \"header\":[\n" +
                "          {\n" +
                "            \"key\":\"Accept\",\n" +
                "            \"type\":\"text\",\n" +
                "            \"value\":\"application/json\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"key\":\"Content-Type\",\n" +
                "            \"type\":\"text\",\n" +
                "            \"value\":\"application/json\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"method\":\"POST\",\n" +
                "        \"url\":\"{{JSON_RPC_ENDPOINT}}\"\n" +
                "      },\n" +
                "      \"response\":[\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"variable\":[\n" +
                "    {\n" +
                "      \"key\":\"JSON_RPC_ENDPOINT\",\n" +
                "      \"value\":\"http://localhost:8080/jsonrpc\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", Files.readString(out));
    }
}
