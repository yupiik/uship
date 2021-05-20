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
