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
package io.yupiik.uship.jsonrpc.client.model;

import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({"jsonrpc", "id", "result", "error"})
public class JsonResponse {
    private String jsonrpc;
    private JsonValue id;
    private JsonValue result;
    private ErrorResponse error;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(final String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public JsonValue getId() {
        return id;
    }

    public void setId(final JsonValue id) {
        this.id = id;
    }

    public JsonValue getResult() {
        return result;
    }

    public void setResult(final JsonValue result) {
        this.result = result;
    }

    public ErrorResponse getError() {
        return error;
    }

    public void setError(final ErrorResponse error) {
        this.error = error;
    }
}
