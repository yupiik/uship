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
package io.yupiik.uship.jsonrpc.doc.openapi;

import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenRPC2OpenAPITest {
    @Test
    void convert() throws Exception {
        try (final var jsonb = JsonbBuilder.create(
            new JsonbConfig().setProperty("johnzon.skip-cdi", true))) {
            final var converter = new OpenRPC2OpenAPI(Json.createBuilderFactory(Map.of()), jsonb);
            assertEquals(""
                    + "{\n" + "  \"openapi\":\"3.0.3\",\n" + "  \"servers\":[\n" + "    {\n"
                    + "      \"url\":\"http://localhost:8080/jsonrpc\",\n"
                    + "      \"description\":\"JSON-RPC API\"\n" + "    }\n" + "  ],\n"
                    + "  \"info\":{\n" + "    \"title\":\"Test\",\n"
                    + "    \"description\":\"The test.\",\n" + "    \"version\":\"1\"\n" + "  },\n"
                    + "  \"paths\":{\n" + "    \"/test1\":{\n" + "      \"post\":{\n"
                    + "        \"operationId\":\"test1\",\n" + "        \"summary\":\"\",\n"
                    + "        \"requestBody\":{\n" + "          \"content\":{\n"
                    + "            \"application/json\":{\n" + "              \"schema\":{\n"
                    + "                \"type\":\"object\",\n" + "                \"properties\":{\n"
                    + "                  \"jsonrpc\":{\n" + "                    \"type\":\"string\",\n"
                    + "                    \"default\":\"2.0\",\n"
                    + "                    \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                  },\n" + "                  \"method\":{\n"
                    + "                    \"type\":\"string\",\n"
                    + "                    \"default\":\"test1\",\n"
                    + "                    \"description\":\"The JSON-RPC method name, should always be 'test1'\"\n"
                    + "                  },\n" + "                  \"params\":{\n"
                    + "                    \"type\":\"object\",\n"
                    + "                    \"properties\":{\n" + "                      \"in\":{\n"
                    + "                        \"type\":\"string\"\n" + "                      }\n"
                    + "                    }\n" + "                  }\n" + "                },\n"
                    + "                \"required\":[\n" + "                  \"jsonrpc\",\n"
                    + "                  \"method\"\n" + "                ]\n" + "              }\n"
                    + "            }\n" + "          }\n" + "        },\n" + "        \"responses\":{\n"
                    + "          \"200\":{\n" + "            \"description\":\"OK\",\n"
                    + "            \"content\":{\n" + "              \"application/json\":{\n"
                    + "                \"schema\":{\n" + "                  \"type\":\"object\",\n"
                    + "                  \"properties\":{\n" + "                    \"jsonrpc\":{\n"
                    + "                      \"type\":\"string\",\n"
                    + "                      \"default\":\"2.0\",\n"
                    + "                      \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                    },\n" + "                    \"result\":{\n"
                    + "                      \"type\":\"object\",\n"
                    + "                      \"properties\":{\n"
                    + "                        \"name\":{\n"
                    + "                          \"type\":\"string\"\n" + "                        }\n"
                    + "                      }\n" + "                    }\n" + "                  },\n"
                    + "                  \"required\":[\n" + "                    \"jsonrpc\",\n"
                    + "                    \"result\"\n" + "                  ]\n"
                    + "                }\n" + "              }\n" + "            }\n" + "          },\n"
                    + "          \"x-jsonrpc-code=1234\":{\n"
                    + "            \"description\":\"When something occurs. (Error code=1234)\",\n"
                    + "            \"content\":{\n" + "              \"application/json\":{\n"
                    + "                \"schema\":{\n" + "                  \"type\":\"object\",\n"
                    + "                  \"properties\":{\n" + "                    \"jsonrpc\":{\n"
                    + "                      \"type\":\"string\",\n"
                    + "                      \"default\":\"2.0\",\n"
                    + "                      \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                    },\n" + "                    \"error\":{\n"
                    + "                      \"type\":\"object\",\n"
                    + "                      \"properties\":{\n"
                    + "                        \"code\":{\n"
                    + "                          \"type\":\"integer\",\n"
                    + "                          \"default\":1234,\n"
                    + "                          \"description\":\"A Number that indicates the error type that occurred. This MUST be an integer.\"\n"
                    + "                        },\n" + "                        \"message\":{\n"
                    + "                          \"type\":\"string\",\n"
                    + "                          \"default\":\"When something occurs.\",\n"
                    + "                          \"description\":\"A String providing a short description of the error. The message SHOULD be limited to a concise single sentence.\"\n"
                    + "                        },\n" + "                        \"data\":{\n"
                    + "                          \"type\":\"object\",\n"
                    + "                          \"properties\":{\n"
                    + "                            \"counter\":{\n"
                    + "                              \"type\":\"integer\"\n"
                    + "                            }\n" + "                          }\n"
                    + "                        }\n" + "                      },\n"
                    + "                      \"required\":[\n" + "                        \"code\",\n"
                    + "                        \"message\"\n" + "                      ]\n"
                    + "                    }\n" + "                  },\n"
                    + "                  \"required\":[\n" + "                    \"jsonrpc\",\n"
                    + "                    \"error\"\n" + "                  ]\n"
                    + "                }\n" + "              }\n" + "            }\n" + "          },\n"
                    + "          \"x-jsonrpc-code=-32600\":{\n"
                    + "            \"description\":\"At least one criteria is required to search customer eligbility. (Error code=-32600)\",\n"
                    + "            \"content\":{\n" + "              \"application/json\":{\n"
                    + "                \"schema\":{\n" + "                  \"type\":\"object\",\n"
                    + "                  \"properties\":{\n" + "                    \"jsonrpc\":{\n"
                    + "                      \"type\":\"string\",\n"
                    + "                      \"default\":\"2.0\",\n"
                    + "                      \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                    },\n" + "                    \"error\":{\n"
                    + "                      \"type\":\"object\",\n"
                    + "                      \"properties\":{\n"
                    + "                        \"code\":{\n"
                    + "                          \"type\":\"integer\",\n"
                    + "                          \"default\":-32600,\n"
                    + "                          \"description\":\"A Number that indicates the error type that occurred. This MUST be an integer.\"\n"
                    + "                        },\n" + "                        \"message\":{\n"
                    + "                          \"type\":\"string\",\n"
                    + "                          \"default\":\"At least one criteria is required to search customer eligbility.\",\n"
                    + "                          \"description\":\"A String providing a short description of the error. The message SHOULD be limited to a concise single sentence.\"\n"
                    + "                        },\n" + "                        \"data\":{\n"
                    + "                          \"type\":\"object\",\n"
                    + "                          \"properties\":{\n"
                    + "                            \"missingParams\":{\n"
                    + "                              \"type\":\"array\",\n"
                    + "                              \"items\":{\n"
                    + "                                \"type\":\"string\"\n"
                    + "                              }\n" + "                            }\n"
                    + "                          },\n" + "                          \"required\":[\n"
                    + "                            \"missingParams\"\n"
                    + "                          ]\n" + "                        }\n"
                    + "                      },\n" + "                      \"required\":[\n"
                    + "                        \"code\",\n" + "                        \"message\"\n"
                    + "                      ]\n" + "                    }\n" + "                  },\n"
                    + "                  \"required\":[\n" + "                    \"jsonrpc\",\n"
                    + "                    \"error\"\n" + "                  ]\n"
                    + "                }\n" + "              }\n" + "            }\n" + "          },\n"
                    + "          \"x-jsonrpc-code=53\":{\n"
                    + "            \"description\":\"At least one criteria is required to search customer eligbility. (Error code=53)\",\n"
                    + "            \"content\":{\n" + "              \"application/json\":{\n"
                    + "                \"schema\":{\n" + "                  \"type\":\"object\",\n"
                    + "                  \"properties\":{\n" + "                    \"jsonrpc\":{\n"
                    + "                      \"type\":\"string\",\n"
                    + "                      \"default\":\"2.0\",\n"
                    + "                      \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                    },\n" + "                    \"error\":{\n"
                    + "                      \"type\":\"object\",\n"
                    + "                      \"properties\":{\n"
                    + "                        \"code\":{\n"
                    + "                          \"type\":\"integer\",\n"
                    + "                          \"default\":53,\n"
                    + "                          \"description\":\"A Number that indicates the error type that occurred. This MUST be an integer.\"\n"
                    + "                        },\n" + "                        \"message\":{\n"
                    + "                          \"type\":\"string\",\n"
                    + "                          \"default\":\"At least one criteria is required to search customer eligbility.\",\n"
                    + "                          \"description\":\"A String providing a short description of the error. The message SHOULD be limited to a concise single sentence.\"\n"
                    + "                        },\n" + "                        \"data\":{\n"
                    + "                          \"type\":\"object\",\n"
                    + "                          \"properties\":{\n"
                    + "                            \"missingParams\":{\n"
                    + "                              \"type\":\"array\",\n"
                    + "                              \"items\":{\n"
                    + "                                \"type\":\"string\"\n"
                    + "                              }\n" + "                            }\n"
                    + "                          },\n" + "                          \"required\":[\n"
                    + "                            \"missingParams\"\n"
                    + "                          ]\n" + "                        }\n"
                    + "                      },\n" + "                      \"required\":[\n"
                    + "                        \"code\",\n" + "                        \"message\"\n"
                    + "                      ]\n" + "                    }\n" + "                  },\n"
                    + "                  \"required\":[\n" + "                    \"jsonrpc\",\n"
                    + "                    \"error\"\n" + "                  ]\n"
                    + "                }\n" + "              }\n" + "            }\n" + "          },\n"
                    + "          \"x-jsonrpc-code=501\":{\n"
                    + "            \"description\":\"At least one criteria is required to search customer eligbility. (Error code=501)\",\n"
                    + "            \"content\":{\n" + "              \"application/json\":{\n"
                    + "                \"schema\":{\n" + "                  \"type\":\"object\",\n"
                    + "                  \"properties\":{\n" + "                    \"jsonrpc\":{\n"
                    + "                      \"type\":\"string\",\n"
                    + "                      \"default\":\"2.0\",\n"
                    + "                      \"description\":\"JSON-RPC version, should always be '2.0'.\"\n"
                    + "                    },\n" + "                    \"error\":{\n"
                    + "                      \"type\":\"object\",\n"
                    + "                      \"properties\":{\n"
                    + "                        \"code\":{\n"
                    + "                          \"type\":\"integer\",\n"
                    + "                          \"default\":501,\n"
                    + "                          \"description\":\"A Number that indicates the error type that occurred. This MUST be an integer.\"\n"
                    + "                        },\n" + "                        \"message\":{\n"
                    + "                          \"type\":\"string\",\n"
                    + "                          \"default\":\"At least one criteria is required to search customer eligbility.\",\n"
                    + "                          \"description\":\"A String providing a short description of the error. The message SHOULD be limited to a concise single sentence.\"\n"
                    + "                        },\n" + "                        \"data\":{\n"
                    + "                          \"type\":\"object\",\n"
                    + "                          \"properties\":{\n"
                    + "                            \"missingParams\":{\n"
                    + "                              \"type\":\"array\",\n"
                    + "                              \"items\":{\n"
                    + "                                \"type\":\"string\"\n"
                    + "                              }\n" + "                            }\n"
                    + "                          },\n" + "                          \"required\":[\n"
                    + "                            \"missingParams\"\n"
                    + "                          ]\n" + "                        }\n"
                    + "                      },\n" + "                      \"required\":[\n"
                    + "                        \"code\",\n" + "                        \"message\"\n"
                    + "                      ]\n" + "                    }\n" + "                  },\n"
                    + "                  \"required\":[\n" + "                    \"jsonrpc\",\n"
                    + "                    \"error\"\n" + "                  ]\n"
                    + "                }\n" + "              }\n" + "            }\n" + "          }\n"
                    + "        }\n" + "      }\n" + "    }\n" + "  },\n" + "  \"tags\":[\n" + "  ],\n"
                    + "  \"components\":{\n" + "    \"schemas\":{\n"
                    + "      \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\":{\n"
                    + "        \"type\":\"object\",\n" + "        \"properties\":{\n"
                    + "          \"name\":{\n" + "            \"type\":\"string\"\n" + "          }\n"
                    + "        }\n" + "      }\n" + "    }\n" + "  }\n" + "}",
                format(converter.apply(getConfiguration(jsonb)).toString()));
        }
    }

    private String format(final String json) {
        final var stringWriter = new StringWriter();
        try (final var reader = Json.createReader(new StringReader(json));
            final var writer = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(stringWriter)) {
            writer.write(reader.read());
        }
        return stringWriter.toString();
    }

    private OpenRPC2OpenAPI.Configuration getConfiguration(final Jsonb jsonb) {
        return new OpenRPC2OpenAPI.Configuration().setTitle("Test").setVersion("1")
            .setDescription("The test.").setOpenRPC(jsonb.fromJson(
                "{\n" + "  \"components\": {\n" + "    \"schemas\": {\n"
                    + "      \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\": {\n"
                    + "        \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\",\n"
                    + "        \"properties\": {\n" + "          \"name\": {\n"
                    + "            \"type\": \"string\"\n" + "          }\n" + "        },\n"
                    + "        \"type\": \"object\"\n" + "      }" + "    }\n" + "  },\n"
                    + "  \"info\": {\n" + "    \"title\": \"JSON-RPC\",\n"
                    + "    \"version\": \"1.0\"\n" + "  },\n" + "  \"methods\": [\n" + "    {\n"
                    + "      \"description\": \"\",\n" + "      \"errors\": [],\n"
                    + "      \"externalDocs\": [],\n" + "      \"links\": [],\n"
                    + "      \"name\": \"test1\",\n" + "      \"paramStructure\": \"either\",\n"
                    + "      \"params\": [\n" + "        {\n" + "          \"description\": \"\",\n"
                    + "          \"name\": \"in\",\n" + "          \"required\": false,\n"
                    + "          \"schema\": {\n" + "            \"type\": \"string\"\n"
                    + "          }\n" + "        }\n" + "      ],\n" + "      \"errors\": [\n"
                    + "        {\n" + "          \"code\": 1234,\n" + "          \"data\": {\n"
                    + "            \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_MyExceptionData\",\n"
                    + "            \"properties\": {\n" + "              \"counter\": {\n"
                    + "                \"type\": \"integer\"\n" + "              }\n"
                    + "            },\n" + "            \"type\": \"object\"\n" + "          },\n"
                    + "          \"message\": \"When something occurs.\"\n" + "        }\n,"
                    + "        {\n" + "          \"code\":-32600,\n" + "          \"data\":{\n"
                    + "            \"$id\":\"com_bfcoi_server_adria_webservices_jsonrpc_model_MissingParam\",\n"
                    + "            \"properties\":{\n" + "              \"missingParams\":{\n"
                    + "                \"items\":{\n" + "                  \"type\":\"string\"\n"
                    + "                },\n" + "                \"type\":\"array\"\n"
                    + "              }\n" + "            },\n" + "            \"required\":[\n"
                    + "              \"missingParams\"\n" + "            ],\n"
                    + "            \"type\":\"object\"\n" + "          },\n"
                    + "          \"message\":\"At least one criteria is required to search customer eligbility.\"\n"
                    + "        }," + "        {\n" + "          \"code\":53,\n"
                    + "          \"data\":{\n"
                    + "            \"$id\":\"com_bfcoi_server_adria_webservices_jsonrpc_model_MissingParam\",\n"
                    + "            \"properties\":{\n" + "              \"missingParams\":{\n"
                    + "                \"items\":{\n" + "                  \"type\":\"string\"\n"
                    + "                },\n" + "                \"type\":\"array\"\n"
                    + "              }\n" + "            },\n" + "            \"required\":[\n"
                    + "              \"missingParams\"\n" + "            ],\n"
                    + "            \"type\":\"object\"\n" + "          },\n"
                    + "          \"message\":\"At least one criteria is required to search customer eligbility.\"\n"
                    + "        }," + "        {\n" + "          \"code\":501,\n"
                    + "          \"data\":{\n"
                    + "            \"$id\":\"com_bfcoi_server_adria_webservices_jsonrpc_model_MissingParam\",\n"
                    + "            \"properties\":{\n" + "              \"missingParams\":{\n"
                    + "                \"items\":{\n" + "                  \"type\":\"string\"\n"
                    + "                },\n" + "                \"type\":\"array\"\n"
                    + "              }\n" + "            },\n" + "            \"required\":[\n"
                    + "              \"missingParams\"\n" + "            ],\n"
                    + "            \"type\":\"object\"\n" + "          },\n"
                    + "          \"message\":\"At least one criteria is required to search customer eligbility.\"\n"
                    + "        }" + "      ],\n" + "      \"result\": {\n"
                    + "        \"name\": \"test1__result\",\n" + "        \"schema\": {\n"
                    + "          \"$id\": \"io_yupiik_uship_jsonrpc_core_servlet_JsonRpcServletTest_Foo\",\n"
                    + "          \"properties\": {\n" + "            \"name\": {\n"
                    + "              \"type\": \"string\"\n" + "            }\n" + "          },\n"
                    + "          \"type\": \"object\"\n" + "        }\n" + "      },\n"
                    + "      \"summary\": \"\",\n" + "      \"tags\": []\n" + "    }\n" + "  ],\n"
                    + "  \"openrpc\": \"1.2.4\",\n" + "  \"servers\": [\n" + "    {\n"
                    + "      \"name\": \"api\",\n" + "      \"summary\": \"JSON-RPC API\",\n"
                    + "      \"url\": \"http://localhost:8080/jsonrpc\",\n"
                    + "      \"variables\": {}\n" + "    }\n" + "  ]\n" + "}", OpenRPC.class));
    }
}
