package io.yupiik.uship.jsonrpc.doc;

import io.yupiik.uship.jsonrpc.core.impl.JsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import java.lang.reflect.InvocationTargetException;

import static io.yupiik.uship.jsonrpc.doc.CliSibling.mapClasses;
import static io.yupiik.uship.jsonrpc.doc.CliSibling.toOutputStream;


public class OpenRPCGenerator {
    private OpenRPCGenerator() {
        // no-op
    }

    public static void main(final String... args) {
        if (args.length < 4) {
            throw new IllegalArgumentException("Usage: java -cp ... " +
                    OpenRPCGenerator.class.getName() + " <title> <jsonrpcclasses> <output> <baseurl> [<formatted>]");
        }
        final var registry = new GeneratingRegistry(args[3]);
        try (final var output = toOutputStream(args[2]);
             final var jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(args.length < 5 || Boolean.parseBoolean(args[4])))) {
            mapClasses(args[1]).forEach(e -> {
                try {
                    registry.registerMethodFromService(e, e.getConstructor().newInstance());
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            output.println(jsonb.toJson(registry.doCreateOpenRpc()));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static class GeneratingRegistry extends JsonRpcMethodRegistry {
        private final String base;

        private GeneratingRegistry(final String base) {
            this.base = base;
        }

        @Override
        public OpenRPC doCreateOpenRpc() {
            return super.doCreateOpenRpc();
        }

        @Override
        protected String getBaseUrl() {
            return base;
        }
    }
}
