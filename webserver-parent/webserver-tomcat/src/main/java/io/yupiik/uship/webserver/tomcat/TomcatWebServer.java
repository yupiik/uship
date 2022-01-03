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
package io.yupiik.uship.webserver.tomcat;

import io.yupiik.uship.webserver.tomcat.loader.LaunchingClassLoaderLoader;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.HandlesTypes;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AbstractAccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.modeler.Registry;

import java.io.CharArrayWriter;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

public class TomcatWebServer implements AutoCloseable {
    private final TomcatWebServerConfiguration configuration;
    private Tomcat tomcat;

    protected TomcatWebServer() {
        this(null);
    }

    public TomcatWebServer(final TomcatWebServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public int getPort() {
        return AbstractProtocol.class.cast(tomcat.getConnector().getProtocolHandler()).getLocalPort();
    }

    public synchronized TomcatWebServer create() {
        if (configuration.isDisableRegistry()) {
            Registry.disableRegistry();
        }

        final var tomcat = createTomcat();
        final var context = createContext();
        tomcat.getHost().addChild(context);
        final var state = context.getState();
        if (state == LifecycleState.STOPPED || state == LifecycleState.FAILED) {
            try {
                close();
            } catch (final RuntimeException re) {
                // no-op
            }
            throw new IllegalStateException("Context didn't start");
        }
        if (configuration.getPort() == 0) {
            configuration.setPort(getPort());
        }

        return this;
    }

    @Override
    public synchronized void close() {
        if (tomcat == null) {
            return;
        }
        try {
            tomcat.stop();
            tomcat.destroy();
            final var server = tomcat.getServer();
            if (server != null) { // give a change to stop the utility executor otherwise it just leaks and stop later
                final var utilityExecutor = server.getUtilityExecutor();
                if (utilityExecutor != null) {
                    try {
                        utilityExecutor.awaitTermination(1, TimeUnit.MINUTES);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (final LifecycleException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Tomcat createTomcat() {
        final var tomcat = newTomcat();
        tomcat.setBaseDir(configuration.getBase());
        tomcat.setPort(configuration.getPort());

        final var host = new StandardHost();
        host.setAutoDeploy(false);
        // note needed to stick to tomcat but neat to enable in customizers: host.setFailCtxIfServletStartFails(true);
        host.setName(configuration.getDefaultHost());
        tomcat.getEngine().addChild(host);

        if (configuration.getTomcatCustomizers() != null) {
            configuration.getTomcatCustomizers().forEach(c -> c.accept(tomcat));
        }
        onTomcat(tomcat);

        try {
            tomcat.init();
        } catch (final LifecycleException e) {
            try {
                tomcat.destroy();
            } catch (final LifecycleException ex) {
                // no-op
            }
            throw new IllegalStateException(e);
        }
        try {
            tomcat.start();
        } catch (final LifecycleException e) {
            close();
            throw new IllegalStateException(e);
        }
        return this.tomcat = tomcat;
    }

    protected StandardContext createContext() {
        final var ctx = newContext();
        ctx.setLoader(new LaunchingClassLoaderLoader());
        ctx.setPath("");
        ctx.setName("");
        ctx.setFailCtxIfServletStartFails(true);
        // ctx.setJarScanner(newSkipScanner()); // we don't use scanning at all with this setup so just ignore useless optims for now
        ctx.addServletContainerInitializer((set, servletContext) -> defaultContextSetup(servletContext), null);
        configuration.getInitializers().forEach(sci -> ctx.addServletContainerInitializer(
                sci, ofNullable(sci.getClass().getAnnotation(HandlesTypes.class)).map(HandlesTypes::value).map(this::scanFor).orElseGet(Set::of)));
        ctx.addLifecycleListener(new Tomcat.FixContextListener());

        final var errorReportValve = new ErrorReportValve();
        errorReportValve.setShowReport(false);
        errorReportValve.setShowServerInfo(false);

        if (configuration.getAccessLogPattern() != null && !configuration.getAccessLogPattern().isBlank()) {
            final var logValve = new AccessLogValve();
            logValve.setPattern(configuration.getAccessLogPattern());
            ctx.getPipeline().addValve(logValve);
        }
        ctx.getPipeline().addValve(errorReportValve);

        // avoid warnings
        ctx.setClearReferencesObjectStreamClassCaches(false);
        ctx.setClearReferencesThreadLocals(false);
        ctx.setClearReferencesRmiTargets(false);

        if (configuration.getContextCustomizers() != null) {
            configuration.getContextCustomizers().forEach(c -> c.accept(ctx));
        }
        onContext(ctx);
        return ctx;
    }

    protected Tomcat newTomcat() {
        return new NoBaseDirTomcat();
    }

    protected StandardContext newContext() {
        return new NoWorkDirContext();
    }

    protected void onTomcat(final Tomcat tomcat) {
        // no-op
    }

    protected void onContext(final StandardContext ctx) {
        // no-op
    }

    protected Set<Class<?>> scanFor(final Class<?>... classes) {
        return Set.of();
    }

    protected void defaultContextSetup(final ServletContext servletContext) {
        if (configuration.isSkipUtf8Filter()) {
            return;
        }
        final var encodingFilter = servletContext.addFilter("default-utf8-filter", (servletRequest, servletResponse, filterChain) -> {
            servletRequest.setCharacterEncoding("UTF-8");
            servletResponse.setCharacterEncoding("UTF-8");
            filterChain.doFilter(servletRequest, servletResponse);
        });
        encodingFilter.setAsyncSupported(true);
        encodingFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    private static class AccessLogValve extends AbstractAccessLogValve {
        private final Logger logger = Logger.getLogger("yupiik.webserver.tomcat.access.log");

        @Override
        protected void log(final CharArrayWriter message) {
            logger.info(message.toString());
        }
    }

    private static class NoBaseDirTomcat extends Tomcat {
        @Override
        protected void initBaseDir() {
            // no-op
        }
    }

    private static class NoWorkDirContext extends StandardContext {
        @Override
        protected void postWorkDirectory() {
            // no-op
        }
    }
}
