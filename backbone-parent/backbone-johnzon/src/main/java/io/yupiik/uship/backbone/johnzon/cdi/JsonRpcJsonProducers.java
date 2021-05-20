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
