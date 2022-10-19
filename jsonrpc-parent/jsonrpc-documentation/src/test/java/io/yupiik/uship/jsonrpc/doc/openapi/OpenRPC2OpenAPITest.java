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
package io.yupiik.uship.jsonrpc.doc.openapi;

import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenRPC2OpenAPITest {
    @Test
    void convert() throws Exception {
        try (final var jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon-.skip-cdi", true))) {
            final var converter = new OpenRPC2OpenAPI(
                    Json.createBuilderFactory(Map.of()),
                    jsonb);
            assertEquals("" +
                            "{\"openapi\":\"3.0.3\",\"servers\":[{\"url\":\"http://localhost:8080/jsonrpc\",\"description\":\"JSON-RPC API\"}]," +
                            "\"info\":{\"title\":\"Test\",\"description\":\"The test.\",\"version\":\"1\"}," +
                            "\"paths\":{\"/test1\":{\"post\":{\"operationId\":\"test1\",\"summary\":\"\"," +
                            "\"requestBody\":{\"content\":{\"application/json\":{\"schema\":{\"type\":\"object\"," +
                            "\"properties\":{\"jsonrpc\":{\"type\":\"string\",\"default\":\"2.0\",\"description\":\"JSON-RPC version, should always be '2.0'.\"}," +
                            "\"method\":{\"type\":\"string\",\"default\":\"test1\",\"description\":\"The JSON-RPC method name, should always be 'test1'\"}," +
                            "\"params\":{\"type\":\"object\",\"properties\":{\"in\":{\"type\":\"string\"}}}},\"required\":[\"jsonrpc\",\"method\"]}}}}," +
                            "\"responses\":{" +
                            "\"200\":{\"description\":\"OK\",\"content\":{\"application/json\":{\"schema\":{\"type\":\"object\",\"properties\":{\"jsonrpc\":{\"type\":\"string\",\"default\":\"2.0\",\"description\":\"JSON-RPC version, should always be '2.0'.\"},\"result\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}},\"required\":[\"jsonrpc\",\"result\"]}}}}," +
                            "\"501\":{\"description\":\"When something occurs. (Error code=1234)\",\"content\":{\"application/json\":{\"schema\":{\"type\":\"object\",\"properties\":{\"jsonrpc\":{\"type\":\"string\",\"default\":\"2.0\",\"description\":\"JSON-RPC version, should always be '2.0'.\"},\"result\":{\"type\":\"object\",\"properties\":{\"counter\":{\"type\":\"integer\"}}}},\"required\":[\"jsonrpc\",\"result\"]}}}}}}}},\"tags\":[],\"components\":{\"schemas\":{\"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}}}}",
                    converter.apply(getConfiguration(jsonb)).toString());
        }
    }

    private OpenRPC2OpenAPI.Configuration getConfiguration(final Jsonb jsonb) {
        return new OpenRPC2OpenAPI.Configuration()
                .setTitle("Test")
                .setVersion("1")
                .setDescription("The test.")
                .setOpenRPC(jsonb.fromJson("{\n" +
                        "  \"components\": {\n" +
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
                        "      \"errors\": [\n" +
                        "        {\n" +
                        "          \"code\": 1234,\n" +
                        "          \"data\": {\n" +
                        "            \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_MyExceptionData\",\n" +
                        "            \"properties\": {\n" +
                        "              \"counter\": {\n" +
                        "                \"type\": \"integer\"\n" +
                        "              }\n" +
                        "            },\n" +
                        "            \"type\": \"object\"\n" +
                        "          },\n" +
                        "          \"message\": \"When something occurs.\"\n" +
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
                        "}", OpenRPC.class));
    }
}
