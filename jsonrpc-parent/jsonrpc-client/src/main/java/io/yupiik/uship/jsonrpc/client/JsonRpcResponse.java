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
package io.yupiik.uship.jsonrpc.client;

import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JsonRpcResponse {
    private final HttpResponse<String> httpResponse;
    private final JsonValue delegate;
    private final Jsonb jsonb;

    JsonRpcResponse(final HttpResponse<String> response, final JsonValue delegate, final Jsonb jsonb) {
        this.httpResponse = response;
        this.delegate = delegate;
        this.jsonb = jsonb;
    }

    public HttpResponse<String> httpResponse() {
        return httpResponse;
    }

    public JsonValue delegate() {
        return delegate;
    }

    public boolean isArray() {
        return delegate.getValueType() == JsonValue.ValueType.ARRAY;
    }

    public boolean isSingle() {
        return delegate.getValueType() == JsonValue.ValueType.OBJECT;
    }

    public Array asArray() {
        return new Array();
    }

    public Single asSingle() {
        return new Single();
    }

    public class Single {
        public boolean hasResult() {
            return delegate.asJsonObject().containsKey("result");
        }

        public boolean isError() {
            return delegate.asJsonObject().containsKey("error");
        }

        public <T> T errorAs(final Class<T> type) {
            return errorAs((Type) type);
        }

        public <T> T errorAs(final Type type) {
            return jsonb.fromJson(new JsonValueReader(delegate.asJsonObject().get("error")), type);
        }

        public <T> T as(final Class<T> type) {
            return as((Type) type);
        }

        public <T> T as(final Type type) {
            return jsonb.fromJson(new JsonValueReader(delegate.asJsonObject().get("result")), type);
        }
    }

    public class Array {
        public boolean hasFailure() {
            return delegate.asJsonArray().stream()
                    .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                    .map(JsonValue::asJsonObject)
                    .anyMatch(it -> it.containsKey("error"));
        }

        public List<Single> list() {
            return all().collect(toList());
        }

        public Stream<Single> all() {
            return delegate
                    .asJsonArray().stream()
                    .map(it -> new JsonRpcResponse(httpResponse, it, jsonb))
                    .map(JsonRpcResponse::asSingle);
        }
    }
}
