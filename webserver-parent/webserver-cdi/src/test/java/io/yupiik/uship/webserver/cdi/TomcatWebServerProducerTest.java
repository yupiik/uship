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
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Cdi(classes = {TomcatWebServerProducerTest.Init.class, TomcatWebServerProducerTest.ConfProducer.class})
class TomcatWebServerProducerTest {
    @Inject
    private TomcatWebServer server;

    @Test
    void serverIsAvailable() throws IOException, InterruptedException {
        assertNotNull(server.getTomcat());

        final var response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder()
                                .GET()
                                .uri(URI.create("http://localhost:" + server.getPort() + "/test"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpServletResponse.SC_OK, response.statusCode());
        assertEquals("ok from servlet", response.body());
    }

    public static class ConfProducer {
        @Produces
        public TomcatWebServerConfiguration configuration() {
            final var configuration = new TomcatWebServerConfiguration();
            configuration.setPort(0);
            return configuration;
        }
    }

    public static class Init implements ServletContainerInitializer {
        @Override
        public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) {
            servletContext.addServlet("test", new HttpServlet() {
                @Override
                protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
                    resp.getWriter().write("ok from servlet");
                }
            }).addMapping("/test");
        }
    }
}
