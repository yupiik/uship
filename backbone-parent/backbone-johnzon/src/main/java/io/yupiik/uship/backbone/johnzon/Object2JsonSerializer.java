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
package io.yupiik.uship.backbone.johnzon;

import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import org.apache.johnzon.jsonb.api.experimental.JsonbExtension;

import java.util.function.Function;

// enables to use johnzon specific API (pre-jsonb v3 hopefully) impl for faster round trips
public class Object2JsonSerializer implements Function<Object, JsonValue> {
    private final Function<Object, JsonValue> delegate;

    public Object2JsonSerializer(final Jsonb jsonb) {
        Function<Object, JsonValue> delegate;
        try {
            if (Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("org.apache.johnzon.jsonb.api.experimental.JsonbExtension")
                    .isInstance(jsonb)) {
                delegate = new JohnzonImpl(jsonb);
            } else {
                delegate = new PortableImpl(jsonb);
            }
        } catch (final ClassNotFoundException e) {
            delegate = new PortableImpl(jsonb);
        }
        this.delegate = delegate;
    }

    @Override
    public JsonValue apply(final Object o) {
        return delegate.apply(o);
    }

    private static class JohnzonImpl implements Function<Object, JsonValue> {
        private final JsonbExtension impl;

        private JohnzonImpl(final Jsonb jsonb) {
            this.impl = JsonbExtension.class.cast(jsonb);
        }

        @Override
        public JsonValue apply(final Object o) {
            return impl.toJsonValue(o);
        }
    }

    private static class PortableImpl implements Function<Object, JsonValue> {
        private final Jsonb impl;

        private PortableImpl(final Jsonb jsonb) {
            this.impl = jsonb;
        }

        @Override
        public JsonValue apply(final Object o) {
            return impl.fromJson(impl.toJson(o), JsonValue.class);
        }
    }
}
