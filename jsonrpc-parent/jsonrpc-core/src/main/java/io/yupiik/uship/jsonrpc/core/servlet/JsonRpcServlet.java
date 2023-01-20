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

import io.yupiik.uship.jsonrpc.core.impl.JsonRpcHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.ServletException;

@ApplicationScoped
public class JsonRpcServlet extends SimpleJsonRpcServlet {
    @Inject
    private JsonRpcHandler handler;

    @Inject
    private Event<JsonRpcBeforeExecution> beforeExecutionEvent;

    @Inject
    private Jsonb jsonb;

    @Override
    public void init() throws ServletException {
        setJsonb(jsonb);
        setHandler(handler);
        setBeforeExecutionEvent(beforeExecutionEvent::fire);
        super.init();
    }
}
