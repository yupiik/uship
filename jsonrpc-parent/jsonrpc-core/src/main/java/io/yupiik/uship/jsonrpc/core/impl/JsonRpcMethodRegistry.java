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
package io.yupiik.uship.jsonrpc.core.impl;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.webserver.tomcat.TomcatWebServer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.ServletContext;
import org.apache.coyote.AbstractProtocol;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class JsonRpcMethodRegistry extends SimpleJsonRpcMethodRegistry {
    @Inject
    private Jsonb jsonb;

    @Inject
    private JsonProvider jsonProvider;

    @Inject
    @JsonRpc
    private Instance<Object> jsonRpcInstances;

    public void doInitAtStartup(@Observes @Initialized(ApplicationScoped.class) final Object init,
                                final Instance<Object> base) {
        super.setJsonb(jsonb);
        super.setJsonProvider(jsonProvider);
        super.setJsonRpcInstances(jsonRpcInstances.stream().collect(toList()));
        if (getBaseUrl() == null) {
            if (ServletContext.class.isInstance(init)) {
                final var ctx = ServletContext.class.cast(init);
                super.setBaseUrl("http://" + ctx.getVirtualServerName() + "/jsonrpc");
            } else {
                super.setBaseUrl(LazyBaseUrlFinder.getBaseUrl(base));
            }
        }
        super.init();
    }

    private static class LazyBaseUrlFinder {
        private LazyBaseUrlFinder() {
            // no-op
        }

        private static String getBaseUrl(final Instance<Object> instance) {
            final var tomcat = TomcatWebServer.class.cast(instance.select(TomcatWebServer.class).get()).getTomcat();
            final var connector = tomcat.getConnector();
            return connector.getScheme() + "://" +
                    tomcat.getHost().getName() + ":" +
                    AbstractProtocol.class.cast(connector.getProtocolHandler()).getLocalPort() +
                    "/jsonrpc";
        }
    }
}
