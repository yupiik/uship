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
package io.yupiik.uship.jsonrpc.quarkus.cdi;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.service.SimpleJsonRpcExtractor;
import io.yupiik.uship.jsonrpc.core.impl.SimpleJsonRpcHandler;
import io.yupiik.uship.jsonrpc.core.impl.SimpleJsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.servlet.JsonRpcBeforeExecution;
import io.yupiik.uship.jsonrpc.core.servlet.SimpleJsonRpcServlet;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.spi.JsonProvider;
import org.eclipse.microprofile.config.Config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

// workaround since quarkus is javax. and not jakarta. so we redefine some beans
@ApplicationScoped
public class JsonRpcBeans {
    @Produces
    @DefaultBean
    @ApplicationScoped
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.skip-cdi", true)
                .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
    }

    public void release(@Disposes final Jsonb jsonb) {
        try {
            jsonb.close();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public JsonProvider jsonProvider() {
        return JsonProvider.provider();
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public SimpleJsonRpcHandler handler(final Jsonb jsonb, final SimpleJsonRpcMethodRegistry registry) {
        return new SimpleJsonRpcHandler() {{
            setJsonb(jsonb);
            setRegistry(registry);
        }};
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public SimpleJsonRpcMethodRegistry registry(final Jsonb jsonb,
                                                final JsonProvider provider,
                                                final Config config,
                                                @JsonRpc final Instance<Object> instances) {
        return new SimpleJsonRpcMethodRegistry() {
            {
                final var endpoints = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                instances.iterator(),
                                Spliterator.IMMUTABLE), false)
                        .map(Object.class::cast)
                        .collect(toList());

                setJsonb(jsonb);
                setJsonProvider(provider);
                setJsonRpcInstances(endpoints);
                setBaseUrl(config.getOptionalValue("jsonrpc.baseUrl", String.class)
                        .orElseGet(() -> "http://localhost:" + config.getOptionalValue("quarkus.http.port", Integer.class).orElse(8080) + "/jsonrpc"));
                init();
            }
        };
    }

    @Produces
    @Unremovable
    @ApplicationScoped
    public SimpleJsonRpcServlet servlet(final Jsonb jsonb, final SimpleJsonRpcHandler handler,
                                        final Event<JsonRpcBeforeExecution> event) {
        return new SimpleJsonRpcServlet() {{
            setJsonb(jsonb);
            setHandler(handler);
            setBeforeExecutionEvent(event::fire);
        }};
    }

    // optional but convenient for security
    @Produces
    @ApplicationScoped
    public SimpleJsonRpcExtractor extractor() {
        return new SimpleJsonRpcExtractor();
    }
}

