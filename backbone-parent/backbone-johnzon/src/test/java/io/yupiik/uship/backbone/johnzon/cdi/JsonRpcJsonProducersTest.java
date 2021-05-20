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
