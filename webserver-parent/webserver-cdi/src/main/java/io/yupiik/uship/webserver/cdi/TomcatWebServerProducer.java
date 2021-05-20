package io.yupiik.uship.webserver.cdi;

import io.yupiik.uship.webserver.tomcat.TomcatWebServer;
import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.servlet.ServletContainerInitializer;
import org.apache.catalina.startup.Tomcat;

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
                                           final Instance<ServletContainerInitializer> initializers) {
        final var serverConfiguration = configuration.isResolvable() ?
                configuration.get() : new TomcatWebServerConfiguration();
        serverConfiguration.setInitializers(Stream.concat(
                serverConfiguration.getInitializers().stream(),
                initializers.stream())
                .distinct().collect(toList()));
        return new TomcatWebServer(serverConfiguration).create();
    }

    public void destroyServer(@Disposes final TomcatWebServer server) {
        server.close();
    }

    @Produces
    public Tomcat tomcat(final TomcatWebServer server) {
        return server.getTomcat();
    }
}
