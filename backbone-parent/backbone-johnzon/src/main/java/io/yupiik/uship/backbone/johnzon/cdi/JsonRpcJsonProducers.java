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
package io.yupiik.uship.backbone.johnzon.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriterFactory;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.spi.JsonProvider;

import java.util.Map;

@ApplicationScoped
public class JsonRpcJsonProducers {
    @Produces
    @ApplicationScoped
    public Jsonb jsonb(final JsonProvider provider) {
        return JsonbBuilder.newBuilder()
                .withProvider(provider)
                .withConfig(new JsonbConfig()
                        .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
                        .setProperty("johnzon.failOnMissingCreatorValues", Boolean.getBoolean("johnzon.failOnMissingCreatorValues")) // for records
                        .setProperty("johnzon.deduplicateObjects", false))
                .build();
    }

    public void releaseJsonb(@Disposes final Jsonb jsonb) {
        try {
            jsonb.close();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @ApplicationScoped
    public JsonProvider provider() {
        return JsonProvider.provider();
    }

    @Produces
    public JsonBuilderFactory jsonBuilderFactory(final JsonProvider provider) {
        return provider.createBuilderFactory(Map.of());
    }

    @Produces
    public JsonReaderFactory jsonReaderFactory(final JsonProvider provider) {
        return provider.createReaderFactory(Map.of());
    }

    @Produces
    public JsonWriterFactory jsonWriterFactory(final JsonProvider provider) {
        return provider.createWriterFactory(Map.of());
    }
}
