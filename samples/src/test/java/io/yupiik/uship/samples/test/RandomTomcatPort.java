package io.yupiik.uship.samples.test;

import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class RandomTomcatPort {
    @Produces
    @ApplicationScoped
    public TomcatWebServerConfiguration configuration() {
        final var configuration = new TomcatWebServerConfiguration();
        configuration.setPort(0);
        return configuration;
    }
}
