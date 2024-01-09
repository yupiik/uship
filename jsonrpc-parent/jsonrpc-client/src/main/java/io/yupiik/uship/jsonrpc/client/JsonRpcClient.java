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
package io.yupiik.uship.jsonrpc.client;

import jakarta.json.JsonArray;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

public class JsonRpcClient implements AutoCloseable {
    private final HttpClient httpClient;
    private final JsonRpcClientConverter protocol;
    private final Sync sync;
    private final ASync async;

    public JsonRpcClient(final JsonRpcClientConfiguration clientConfiguration) {
        this.httpClient = ofNullable(clientConfiguration.getHttpClient()).orElseGet(HttpClient::newHttpClient);
        this.protocol = new JsonRpcClientConverter(clientConfiguration);
        this.sync = new Sync();
        this.async = new ASync();
    }

    public Sync sync() {
        return sync;
    }

    public ASync async() {
        return async;
    }

    public JsonRpcClientConverter protocol() {
        return protocol;
    }

    @Override
    public void close() {
        try {
            protocol.close();
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private abstract class BaseClient implements Client {
        @Override
        public JsonRpcClientConverter protocol() {
            return protocol;
        }

        @Override
        public JsonRpcClient root() {
            return JsonRpcClient.this;
        }
    }

    public class Sync extends BaseClient {
        public JsonRpcResponse execute(final JsonArray bulkRequest) throws IOException, InterruptedException {
            return protocol.toJsonRpcResponse(httpClient.send(protocol.toHttpRequest(bulkRequest), HttpResponse.BodyHandlers.ofString(UTF_8)));
        }

        public JsonRpcResponse execute(final String method, final Object params) throws IOException, InterruptedException {
            return protocol.toJsonRpcResponse(httpClient.send(protocol.toHttpRequest(method, params), HttpResponse.BodyHandlers.ofString(UTF_8)));
        }
    }

    public class ASync extends BaseClient {
        public CompletableFuture<JsonRpcResponse> execute(final JsonArray bulkRequest) {
            return httpClient.sendAsync(protocol.toHttpRequest(bulkRequest), HttpResponse.BodyHandlers.ofString(UTF_8))
                    .thenApply(protocol::toJsonRpcResponse);
        }

        public CompletableFuture<JsonRpcResponse> execute(final String method, final Object params) {
            return httpClient.sendAsync(protocol.toHttpRequest(method, params), HttpResponse.BodyHandlers.ofString(UTF_8))
                    .thenApply(protocol::toJsonRpcResponse);
        }
    }

    public interface Client {
        JsonRpcClientConverter protocol();

        JsonRpcClient root();
    }
}
