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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
}
