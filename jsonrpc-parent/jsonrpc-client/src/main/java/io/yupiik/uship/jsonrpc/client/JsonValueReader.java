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

import jakarta.json.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

class JsonValueReader extends Reader implements Supplier<JsonValue> {
    private final JsonValue input;
    private ByteArrayInputStream fallbackDelegate;

    public JsonValueReader(final JsonValue input) {
        this.input = input;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) {
        if (fallbackDelegate == null) {
            fallbackDelegate = new ByteArrayInputStream(input.toString().getBytes(StandardCharsets.UTF_8));
        }
        return fallbackDelegate.read();
    }

    @Override
    public void close() throws IOException {
        if (fallbackDelegate != null) {
            fallbackDelegate.close();
        }
    }

    @Override
    public JsonValue get() {
        return input;
    }
}