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
package io.yupiik.uship.jsonrpc.spring.impl;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.service.SimpleJsonRpcExtractor;
import io.yupiik.uship.jsonrpc.core.impl.SimpleJsonRpcHandler;
import io.yupiik.uship.jsonrpc.core.impl.SimpleJsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.servlet.SimpleJsonRpcServlet;
import io.yupiik.uship.jsonrpc.spring.JsonRpcConfiguration;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.spi.JsonProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(JsonRpcConfiguration.class)
public class JsonRpcBeans {
    @Bean
    ServletRegistrationBean<BridgeJakarta2JavaxServlet> jsonRpcServlet(final Jsonb jsonb,
                                                                       final SimpleJsonRpcHandler handler,
                                                                       final ApplicationContext context,
                                                                       final JsonRpcConfiguration configuration) {
        final var servlet = new SimpleJsonRpcServlet();
        servlet.setJsonb(jsonb);
        servlet.setHandler(handler);
        servlet.setBeforeExecutionEvent(context::publishEvent);
        return new ServletRegistrationBean<>(new BridgeJakarta2JavaxServlet(servlet), configuration.getBinding());
    }

    @Bean
    SimpleJsonRpcMethodRegistry jsonRpcRegistry(final ApplicationContext context,
                                                final JsonRpcConfiguration configuration) {
        final var endpoints = context.getBeansWithAnnotation(JsonRpc.class).values();
        final var registry = new SpringJsonRpcMethodRegistry();
        registry.setJsonb(jsonb());
        registry.setJsonProvider(jsonProvider());
        registry.setJsonRpcInstances(endpoints);
        registry.setBaseUrl(configuration.getBaseUrl());
        return registry;
    }

    @Bean
    SimpleJsonRpcExtractor jsonRpcExtractor() {
        return new SimpleJsonRpcExtractor();
    }

    @Bean
    SimpleJsonRpcHandler jsonRpcHandler() {
        final var handler = new SimpleJsonRpcHandler();
        handler.setRegistry(jsonRpcRegistry(null, null));
        handler.setJsonb(jsonb());
        return handler;
    }

    @Bean
    @ConditionalOnMissingBean
    JsonProvider jsonProvider() {
        return JsonProvider.provider();
    }

    @Bean
    @ConditionalOnMissingBean
    Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig()
                .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
    }

    private static class SpringJsonRpcMethodRegistry extends SimpleJsonRpcMethodRegistry {
        @Autowired
        private JsonRpcConfiguration configuration;

        @EventListener
        public void onStart(final WebServerInitializedEvent initializedEvent) {
            if ("auto".equals(configuration.getBaseUrl())) {
                setBaseUrl("http://localhost:" + initializedEvent.getWebServer().getPort() + configuration.getBinding());
            }
            super.init();
        }
    }
}
