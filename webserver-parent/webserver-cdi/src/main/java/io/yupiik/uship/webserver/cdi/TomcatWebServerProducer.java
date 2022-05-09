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
package io.yupiik.uship.webserver.cdi;

import io.yupiik.uship.webserver.tomcat.TomcatWebServer;
import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import io.yupiik.uship.webserver.tomcat.customizer.ContextCustomizer;
import io.yupiik.uship.webserver.tomcat.customizer.TomcatCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.servlet.ServletContainerInitializer;
import org.apache.catalina.startup.Tomcat;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class TomcatWebServerProducer {
    public void start(@Observes @Initialized(ApplicationScoped.class) final Object init,
                      final TomcatWebServer server) {
        server.getTomcat(); // force init
    }

    @Produces
    @ApplicationScoped
    public TomcatWebServer tomcatWebServer(final Instance<TomcatWebServerConfiguration> configuration,
                                           final Instance<ServletContainerInitializer> initializers,
                                           final Instance<TomcatCustomizer> tomcatCustomizers,
                                           final Instance<ContextCustomizer> contextCustomizers) {
        final var serverConfiguration = configuration.isResolvable() ?
                configuration.get() : new TomcatWebServerConfiguration();
        serverConfiguration.setInitializers(Stream.concat(
                serverConfiguration.getInitializers().stream(),
                initializers.stream())
                .distinct().collect(toList()));
        serverConfiguration.setTomcatCustomizers(merge(serverConfiguration.getTomcatCustomizers(), tomcatCustomizers));
        serverConfiguration.setContextCustomizers(merge(serverConfiguration.getContextCustomizers(), contextCustomizers));
        return new TomcatWebServer(serverConfiguration).create();
    }

    public void destroyServer(@Disposes final TomcatWebServer server) {
        server.close();
    }

    @Produces
    public Tomcat tomcat(final TomcatWebServer server) {
        return server.getTomcat();
    }

    private <T> List<Consumer<T>> merge(final List<Consumer<T>> first, final Instance<? extends Consumer<T>> second) {
        return Stream.concat(
                first != null ? first.stream() : Stream.empty(),
                second.stream())
                .collect(toList());
    }
}
