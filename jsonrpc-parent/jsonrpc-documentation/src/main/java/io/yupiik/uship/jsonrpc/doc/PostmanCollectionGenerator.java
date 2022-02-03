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
import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import io.yupiik.uship.jsonrpc.doc.postman.PostmanCollection;
import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class PostmanCollectionGenerator implements Runnable {
    private final JsonGeneratorFactory jsonGeneratorFactory;
    private final Jsonb jsonb;
    private final OpenRPC openrpc;
    private final PrintStream output;

    public PostmanCollectionGenerator(final Reader openrpc,
                                      final PrintStream output,
                                      final Jsonb jsonb,
                                      final JsonGeneratorFactory jsonGeneratorFactory) {
        this.jsonb = jsonb;
        this.jsonGeneratorFactory = jsonGeneratorFactory;
        this.openrpc = jsonb.fromJson(openrpc, OpenRPC.class);
        this.output = output;
    }

    @Override
    public void run() {
        final var items = ofNullable(openrpc.getMethods()).orElseGet(List::of).stream()
                .map(this::toPostman)
                .collect(toList());

        final var info = new PostmanCollection.Info();
        if (openrpc.getInfo() != null) {
            info.setName(openrpc.getInfo().getTitle());
            info.setVersion(openrpc.getInfo().getVersion());
        }

        final var endpointVariable = new PostmanCollection.Variable();
        endpointVariable.setKey("JSON_RPC_ENDPOINT");
        endpointVariable.setValue(ofNullable(openrpc.getServers())
                .orElseGet(List::of).stream()
                .map(OpenRPC.Server::getUrl)
                .findFirst()
                .orElse("http://localhost:8080/jsonrpc"));

        final var root = new PostmanCollection();
        root.setInfo(info);
        root.setItems(items);
        root.setVariable(List.of(endpointVariable));

        jsonb.toJson(root, output);
    }

    private PostmanCollection.Item toPostman(final OpenRPC.RpcMethod registration) {
        final var jsonAcceptHeader = new PostmanCollection.Header();
        jsonAcceptHeader.setKey("Accept");

        final var jsonContentTypeHeader = new PostmanCollection.Header();
        jsonContentTypeHeader.setKey("Content-Type");

        Stream.of(jsonAcceptHeader, jsonContentTypeHeader).forEach(h -> {
            h.setValue("application/json");
            h.setType("text");
        });

        final var body = new PostmanCollection.Body();
        body.setMode("raw");
        body.setRaw(toSampleRequest(registration.getName(), registration.getParams()));

        final var request = new PostmanCollection.Request();
        request.setMethod("POST");
        request.setUrl("{{JSON_RPC_ENDPOINT}}");
        request.setHeader(List.of(jsonAcceptHeader, jsonContentTypeHeader));
        request.setBody(body);

        final var item = new PostmanCollection.Item();
        item.setName(registration.getName());
        item.setRequest(request);
        item.setResponse(List.of() /* todo */);
        return item;
    }

    private String toSampleRequest(final String jsonRpcMethod, final Collection<OpenRPC.Value> parameters) {
        final var out = new StringWriter();
        try (final var gen = jsonGeneratorFactory.createGenerator(out)) {
            gen.writeStartObject();
            gen.write("jsonrpc", "2.0");
            gen.write("method", jsonRpcMethod);
            if (!parameters.isEmpty()) {
                gen.writeStartObject("params");
                parameters.forEach(p -> toSampleParam(p.getName(), p.getSchema(), gen));
                gen.writeEnd();
            }
            gen.writeEnd();
        }
        return out.toString();
    }

    private void toSampleParam(final String name, final Schema schema, final JsonGenerator generator) {
        if (schema == null) {
            return;
        }
        switch (schema.getType()) {
            case string:
                generator.write(
                        name,
                        schema.getExample() != null ? schema.getExample().toString() : "string");
                break;
            case number:
            case integer:
                generator.write(
                        name,
                        schema.getExample() != null ? Number.class.cast(schema.getExample()).doubleValue() : 123);
                break;
            case bool:
                generator.write(
                        name,
                        schema.getExample() != null ? Boolean.class.cast(schema.getExample()) : true);
                break;
            case array:
                generator.writeStartArray(name);
                final var items = schema.getItems();
                if (items != null) {
                    switch (items.getType()) {
                        case string:
                            generator.write(items.getExample() != null ? items.getExample().toString() : "string");
                            break;
                        case number:
                        case integer:
                            generator.write(items.getExample() != null ? Number.class.cast(items.getExample()).doubleValue() : 123);
                            break;
                        case bool:
                            generator.write(items.getExample() != null ? Boolean.class.cast(items.getExample()) : true);
                            break;
                        case array:
                            generator.writeStartArray(name);
                            // todo or too complex in the request?
                            generator.writeEnd();
                            break;
                        case object:
                            generator.writeStartObject();
                            if (items.getProperties() != null) {
                                items.getProperties().forEach((n, s) -> toSampleParam(n, s, generator));
                            }
                            generator.writeEnd();
                            break;
                        default:
                            // no-op
                    }
                }
                generator.writeEnd();
                break;
            case object:
                generator.writeStartObject(name);
                if (schema.getProperties() != null) {
                    schema.getProperties().forEach((n, s) -> toSampleParam(n, s, generator));
                }
                generator.writeEnd();
                break;
            default:
                // no-op
        }
    }

    public static void main(final String... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java -cp ... " +
                    PostmanCollectionGenerator.class.getName() + " <openrpc path or stdin> <output>");
        }
        try (final var input = CliSibling.toReader(args[0]);
             final var output = CliSibling.toOutputStream(args[1]);
             final var jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            new PostmanCollectionGenerator(
                    input, output, jsonb,
                    Json.createGeneratorFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))).run();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
