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

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

import java.util.Set;

@Dependent
public class JsonRpcServletRegistration implements ServletContainerInitializer {
    @Inject
    private JsonRpcServlet handler;

    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) {
        final var jsonrpc = servletContext.addServlet("jsonrpc", handler);
        jsonrpc.setLoadOnStartup(1);
        jsonrpc.setAsyncSupported(true);
        jsonrpc.addMapping("/jsonrpc");
    }
}
