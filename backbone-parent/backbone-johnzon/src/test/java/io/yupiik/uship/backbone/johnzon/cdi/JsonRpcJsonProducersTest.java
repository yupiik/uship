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
package io.yupiik.uship.backbone.johnzon.cdi;

import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriterFactory;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

@Cdi
class JsonRpcJsonProducersTest {
    @Inject
    private JsonProvider provider;

    @Inject
    private Jsonb jsonb;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Inject
    private JsonReaderFactory jsonReaderFactory;

    @Inject
    private JsonWriterFactory jsonWriterFactory;

    @Test
    void exist() {
        Stream.of(provider, jsonb, jsonBuilderFactory, jsonReaderFactory, jsonWriterFactory)
                .forEach(Assertions::assertNotNull);
    }
}
