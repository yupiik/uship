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
package io.yupiik.uship.jsonrpc.core.servlet;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import io.yupiik.uship.jsonrpc.core.protocol.JsonRpcException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Cdi(classes = JsonRpcServletTest.Endpoints.class)
class JsonRpcServletTest {
    @Inject
    private JsonRpcServlet handler;

    @Test
    void missingJsonRpc() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"method\":\"test1\",\"params\":[\"niamor\"]}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600,\"message\":\"Missing jsonrpc\"}}", result);
    }

    @Test
    void missingMethod() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\"}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32601,\"message\":\"Missing method\"}}", result);
    }

    @Test
    void emptyMethod() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"\"}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32601,\"message\":\"Empty method\"}}", result);
    }

    @Test
    void simple() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test1\",\"params\":[\"niamor\"]}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"result\":{\"name\":\"romain\"}}", result);
    }

    @Test
    void businessErrorDirect() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test3\"}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":1234,\"message\":\"oops\"}}", result);
    }

    @Test
    void businessErrorPromise() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test4\"}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":1234,\"message\":\"oops\"}}", result);
    }

    @Test
    void businessErrorNotJsonRpcException() throws IOException, ServletException {
        {
            final var servlet = new ServletSimulator();
            final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test5\"}", HttpServletResponse.SC_OK);
            assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"oops\"}}", result);
        }
        {
            final var servlet = new ServletSimulator();
            final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test6\"}", HttpServletResponse.SC_OK);
            assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"oops\"}}", result);
        }
    }

    @Test
    void protocolError() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test3\"", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32700,\"message\":\"Unexpected character '\\\"' (Codepoint: 34) on [lineNumber=1, columnNumber=67, streamOffset=66]. Reason is [[End of file hit too early]]\"}}", result);
    }

    @Test
    void invalidMethod() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "{\"jsonrpc\":\"2.0\",\"method\":\"test_missing\"}", HttpServletResponse.SC_OK);
        assertEquals("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32601,\"message\":\"Unknown method (test_missing)\"}}", result);
    }

    @Test
    void bulk() throws IOException, ServletException {
        final var servlet = new ServletSimulator();
        final var result = servlet.serve(handler, "[" +
                "{\"jsonrpc\":\"2.0\",\"method\":\"test1\",\"params\":[\"niamor\"]}," +
                "{\"jsonrpc\":\"2.0\",\"method\":\"test2\",\"params\":[\"francois\"]}" +
                "]", HttpServletResponse.SC_OK);
        assertEquals("[" +
                "{\"jsonrpc\":\"2.0\",\"result\":{\"name\":\"romain\"}}," +
                "{\"jsonrpc\":\"2.0\",\"result\":\"francois\"}" +
                "]", result);
    }

    public static class Foo {
        private String name;

        public Foo() {
            // no-op
        }

        public Foo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @JsonRpc
    public static class Endpoints {
        @JsonRpcMethod(name = "test1")
        public Foo test1(@JsonRpcParam final String in) {
            return new Foo(new StringBuilder(in).reverse().toString());
        }

        @JsonRpcMethod(name = "test2")
        public String test2(@JsonRpcParam final String in) {
            return in;
        }

        @JsonRpcMethod(name = "test3")
        public String test3() {
            throw new JsonRpcException(1234, "oops");
        }

        @JsonRpcMethod(name = "test4")
        public CompletionStage<Object> test4() {
            final var promise = new CompletableFuture<>();
            promise.completeExceptionally(new JsonRpcException(1234, "oops"));
            return promise;
        }

        @JsonRpcMethod(name = "test5")
        public String test5() {
            throw new IllegalStateException("oops");
        }

        @JsonRpcMethod(name = "test6")
        public CompletionStage<Object> test6() {
            final var promise = new CompletableFuture<>();
            promise.completeExceptionally(new IllegalStateException("oops"));
            return promise;
        }
    }
}
