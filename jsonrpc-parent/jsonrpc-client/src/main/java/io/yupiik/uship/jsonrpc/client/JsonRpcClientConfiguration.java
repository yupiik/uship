/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.bind.Jsonb;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.function.Function;

public class JsonRpcClientConfiguration {
    private final String endpoint;
    private HttpClient httpClient;
    private JsonBuilderFactory jsonBuilderFactory;
    private Jsonb jsonb;
    private Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomizer;

    public JsonRpcClientConfiguration(final String endpoint) {
        this.endpoint = endpoint;
    }


    public JsonBuilderFactory getJsonBuilderFactory() {
        return jsonBuilderFactory;
    }

    public JsonRpcClientConfiguration setJsonBuilderFactory(final JsonBuilderFactory jsonBuilderFactory) {
        this.jsonBuilderFactory = jsonBuilderFactory;
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public JsonRpcClientConfiguration setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public Function<HttpRequest.Builder, HttpRequest.Builder> getRequestCustomizer() {
        return requestCustomizer;
    }

    public JsonRpcClientConfiguration setRequestCustomizer(final Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomizer) {
        this.requestCustomizer = requestCustomizer;
        return this;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public Jsonb getJsonb() {
        return jsonb;
    }

    public JsonRpcClientConfiguration setJsonb(final Jsonb jsonb) {
        this.jsonb = jsonb;
        return this;
    }
}
