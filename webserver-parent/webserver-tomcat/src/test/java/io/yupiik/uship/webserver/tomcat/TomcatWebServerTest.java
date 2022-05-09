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
package io.yupiik.uship.webserver.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TomcatWebServerTest {
    @Test
    void run() throws IOException, InterruptedException {
        final var configuration = new TomcatWebServerConfiguration();
        configuration.setPort(0); // random
        configuration.setInitializers(List.of((set, servletContext) ->
                servletContext.addServlet("test", new HttpServlet() {
                    @Override
                    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
                        resp.getWriter().write("ok from servlet");
                    }
                }).addMapping("/test")));
        try (final var server = new TomcatWebServer(configuration).create()) {
            // port was updated (cause it was 0)
            assertNotEquals(0, configuration.getPort());
            assertEquals(configuration.getPort(), server.getPort());

            // server is listening
            final var client = HttpClient.newHttpClient();
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + server.getPort() + "/test"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpServletResponse.SC_OK, response.statusCode());
            assertEquals("ok from servlet", response.body());
        }
    }

    @Test
    void stopOnFailure() {
        final var configuration = new TomcatWebServerConfiguration();
        configuration.setPort(0); // random
        configuration.setInitializers(List.of((set, servletContext) -> {
            final var servlet = servletContext.addServlet("test", new HttpServlet() {
                @Override
                public void init() {
                    throw new IllegalArgumentException();
                }
            });
            servlet.setLoadOnStartup(1);
            servlet.addMapping("/test");
        }));
        final var threadsBefore = listThreads();
        final var server = new TomcatWebServer(configuration);
        assertThrows(IllegalStateException.class, server::create);
        assertEquals(Set.of(threadsBefore), Set.of(listThreads()));
    }

    private Thread[] listThreads() {
        final var threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        return threads;
    }
}
