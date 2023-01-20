/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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

import io.yupiik.uship.jsonrpc.core.impl.SimpleJsonRpcHandler;
import io.yupiik.uship.jsonrpc.core.protocol.JsonRpcException;
import jakarta.json.JsonException;
import jakarta.json.JsonStructure;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleJsonRpcServlet extends HttpServlet {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private SimpleJsonRpcHandler handler;
    private Consumer<JsonRpcBeforeExecution> beforeExecutionEvent;
    private Jsonb jsonb;

    protected void setHandler(final SimpleJsonRpcHandler handler) {
        this.handler = handler;
    }

    protected void setBeforeExecutionEvent(final Consumer<JsonRpcBeforeExecution> beforeExecutionEvent) {
        this.beforeExecutionEvent = beforeExecutionEvent;
    }

    protected void setJsonb(final Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final JsonStructure request;
        try {
            request = handler.readRequest(req, req.getReader());
        } catch (final JsonbException | JsonException jsonbEx) {
            forwardResponse(handler.createResponse(null, -32700, jsonbEx.getMessage()), resp);
            return;
        }

        try {
            beforeExecutionEvent.accept(new JsonRpcBeforeExecution(request, req));
        } catch (final JsonRpcException re) {
            forwardResponse(handler.toErrorResponse(null, re, request), resp);
            return;
        } catch (final RuntimeException re) {
            forwardResponse(handler.createResponse(null, 100, re.getMessage()), resp);
            return;
        }

        final var ctx = req.startAsync();
        handler.execute(request, req, resp).whenComplete((value, error) -> {
            try {
                if (value != null) {
                    forwardResponse(value, resp);
                } else {
                    forwardResponse(handler.createResponse(null, -32603, error.getMessage()), resp);
                }
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalStateException(e);
            } catch (final RuntimeException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            } finally {
                ctx.complete();
            }
        });
    }

    private void forwardResponse(final Object payload, final HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.addHeader("content-type", "application/json;charset=utf-8");
        try (final var out = resp.getWriter()) {
            jsonb.toJson(payload, out);
        }
    }
}
