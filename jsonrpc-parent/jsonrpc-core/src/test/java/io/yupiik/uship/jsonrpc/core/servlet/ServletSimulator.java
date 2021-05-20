package io.yupiik.uship.jsonrpc.core.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServletSimulator {
    private final Request request;
    private final Response response;

    protected ServletSimulator(final Request request, final Response response) {
        this.request = request;
        this.response = response;
    }

    public ServletSimulator() {
        this(new Request(new Connector()), new Response());

        final var context = new StandardContext();
        context.setCookieProcessor(new Rfc6265CookieProcessor());

        request.setCoyoteRequest(new org.apache.coyote.Request());
        request.setResponse(response);
        request.getMappingData().context = context;

        response.setCoyoteResponse(new org.apache.coyote.Response());
        response.setRequest(request);
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public String serve(final HttpServlet servlet, final String requestPayload, final int expectedStatus) throws ServletException, IOException {
        final var out = new StringWriter();
        servlet.service(new HttpServletRequestWrapper(getRequest()) {
            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new StringReader(requestPayload));
            }
        }, new HttpServletResponseWrapper(getResponse()) {
            @Override
            public PrintWriter getWriter() {
                return new PrintWriter(out);
            }
        });
        assertEquals(expectedStatus, response.getStatus());
        return out.toString();
    }
}
