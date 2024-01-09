/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.quarkus.servlet;

import io.yupiik.uship.jakartajavax.bridge.BridgeJakarta2JavaxServlet;
import io.yupiik.uship.jsonrpc.core.servlet.SimpleJsonRpcServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Set;

public class JsonRpcServletInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) {
        final var servlet = ctx.addServlet("jsonrpc", new BridgeJakarta2JavaxServlet(new HttpServlet() {
            // lazy init due to the quarkus lifecycle between undertow and arc and the lack of correct injections support for undertow components
            private volatile SimpleJsonRpcServlet servlet;

            @Override
            protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
                lazyLookup();
                servlet.service(req, resp);
            }

            private void lazyLookup() {
                if (servlet == null) {
                    synchronized (this) {
                        if (servlet == null) {
                            servlet = CDI.current().select(SimpleJsonRpcServlet.class).get();
                        }
                    }
                }
            }
        }));
        servlet.addMapping(ConfigProvider.getConfig().getOptionalValue("jsonrpc.binding", String.class).orElse("/jsonrpc"));
        servlet.setAsyncSupported(true);
    }
}
