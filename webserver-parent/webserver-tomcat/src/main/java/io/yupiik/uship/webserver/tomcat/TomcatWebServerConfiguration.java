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

import jakarta.servlet.ServletContainerInitializer;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

import java.util.List;
import java.util.function.Consumer;

public class TomcatWebServerConfiguration {
    private boolean skipUtf8Filter = false;
    private boolean disableRegistry = true;
    private String base = "";
    private int port = 8080;
    private String defaultHost = "localhost";
    private String accessLogPattern = "common";
    private List<ServletContainerInitializer> initializers = List.of();
    private List<Consumer<Tomcat>> tomcatCustomizers = List.of();
    private List<Consumer<StandardContext>> contextCustomizers = List.of();

    public boolean isSkipUtf8Filter() {
        return skipUtf8Filter;
    }

    public void setSkipUtf8Filter(final boolean skipUtf8Filter) {
        this.skipUtf8Filter = skipUtf8Filter;
    }

    public void setTomcatCustomizers(final List<Consumer<Tomcat>> tomcatCustomizers) {
        this.tomcatCustomizers = tomcatCustomizers;
    }

    public void setContextCustomizers(final List<Consumer<StandardContext>> contextCustomizers) {
        this.contextCustomizers = contextCustomizers;
    }

    public void setDisableRegistry(final boolean disableRegistry) {
        this.disableRegistry = disableRegistry;
    }

    public void setBase(final String base) {
        this.base = base;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setDefaultHost(final String defaultHost) {
        this.defaultHost = defaultHost;
    }

    public void setAccessLogPattern(final String accessLogPattern) {
        this.accessLogPattern = accessLogPattern;
    }

    public void setInitializers(final List<ServletContainerInitializer> initializers) {
        this.initializers = initializers;
    }

    public boolean isDisableRegistry() {
        return disableRegistry;
    }

    public String getBase() {
        return base;
    }

    public int getPort() {
        return port;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public String getAccessLogPattern() {
        return accessLogPattern;
    }

    public List<ServletContainerInitializer> getInitializers() {
        return initializers;
    }

    public List<Consumer<Tomcat>> getTomcatCustomizers() {
        return tomcatCustomizers;
    }

    public List<Consumer<StandardContext>> getContextCustomizers() {
        return contextCustomizers;
    }
}
