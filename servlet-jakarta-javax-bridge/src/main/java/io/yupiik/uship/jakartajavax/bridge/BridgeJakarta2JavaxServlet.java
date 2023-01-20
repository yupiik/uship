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
package io.yupiik.uship.jakartajavax.bridge;


import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionContext;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class BridgeJakarta2JavaxServlet extends HttpServlet {
    private final jakarta.servlet.http.HttpServlet delegate;
    private jakarta.servlet.ServletConfig configBridge;
    private jakarta.servlet.ServletContext contextBridge;

    public BridgeJakarta2JavaxServlet(final jakarta.servlet.http.HttpServlet delegate) {
        this.delegate = delegate;
    }

    @Override
    public void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try {
            delegate.service(new BridgeRequest(req, contextBridge), new BridgeResponse(resp));
        } catch (final jakarta.servlet.ServletException e) {
            throw new ServletException(e.getMessage(), e.getRootCause());
        }
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws IOException, ServletException {
        service(HttpServletRequest.class.cast(req), HttpServletResponse.class.cast(res));
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    @Override
    public String getInitParameter(final String name) {
        return delegate.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return delegate.getInitParameterNames();
    }

    @Override
    public String getServletInfo() {
        return delegate.getServletInfo();
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.contextBridge = new BridgeServletContext(config.getServletContext());
        this.configBridge = new BridgeServletConfig(config, contextBridge);
        try {
            delegate.init(configBridge);
        } catch (final jakarta.servlet.ServletException e) {
            throw new ServletException(e.getMessage(), e.getRootCause());
        }
    }

    @Override
    public void init() throws ServletException {
        try {
            delegate.init();
        } catch (final jakarta.servlet.ServletException e) {
            throw new ServletException(e.getMessage(), e.getRootCause());
        }
    }

    @Override
    public void log(final String message) {
        delegate.log(message);
    }

    @Override
    public void log(final String message, final Throwable t) {
        delegate.log(message, t);
    }

    @Override
    public String getServletName() {
        return delegate.getServletName();
    }

    private static class BridgeServletConfig implements jakarta.servlet.ServletConfig {
        private final ServletConfig delegate;
        private final jakarta.servlet.ServletContext context;

        private BridgeServletConfig(final ServletConfig config, final jakarta.servlet.ServletContext context) {
            this.delegate = config;
            this.context = context;
        }

        @Override
        public String getServletName() {
            return delegate.getServletName();
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return context;
        }

        @Override
        public String getInitParameter(String s) {
            return delegate.getInitParameter(s);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return delegate.getInitParameterNames();
        }
    }

    private static class BridgeSession implements jakarta.servlet.http.HttpSession {
        private final javax.servlet.http.HttpSession delegate;
        private final jakarta.servlet.ServletContext context;

        private BridgeSession(final javax.servlet.http.HttpSession delegate, final jakarta.servlet.ServletContext context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public long getCreationTime() {
            return delegate.getCreationTime();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public long getLastAccessedTime() {
            return delegate.getLastAccessedTime();
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return context;
        }

        @Override
        public void setMaxInactiveInterval(int i) {
            delegate.setMaxInactiveInterval(i);
        }

        @Override
        public int getMaxInactiveInterval() {
            return delegate.getMaxInactiveInterval();
        }

        @Override
        public HttpSessionContext getSessionContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String s) {
            return delegate.getAttribute(s);
        }

        @Override
        public Object getValue(String s) {
            return delegate.getValue(s);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return delegate.getAttributeNames();
        }

        @Override
        public String[] getValueNames() {
            return delegate.getValueNames();
        }

        @Override
        public void setAttribute(final String s, final Object o) {
            delegate.setAttribute(s, o);
        }

        @Override
        public void putValue(final String s, final Object o) {
            delegate.putValue(s, o);
        }

        @Override
        public void removeAttribute(final String s) {
            delegate.removeAttribute(s);
        }

        @Override
        public void removeValue(final String s) {
            delegate.removeValue(s);
        }

        @Override
        public void invalidate() {
            delegate.invalidate();
        }

        @Override
        public boolean isNew() {
            return delegate.isNew();
        }
    }

    private static class BridgeServletContext implements jakarta.servlet.ServletContext {
        private final ServletContext delegate;

        private BridgeServletContext(final ServletContext context) {
            this.delegate = context;
        }

        @Override
        public String getContextPath() {
            return delegate.getContextPath();
        }

        @Override
        public jakarta.servlet.ServletContext getContext(final String s) {
            final var context = delegate.getContext(s);
            return context == null ? null : new BridgeServletContext(context);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public int getEffectiveMajorVersion() {
            return delegate.getEffectiveMajorVersion();
        }

        @Override
        public int getEffectiveMinorVersion() {
            return delegate.getEffectiveMinorVersion();
        }

        @Override
        public String getMimeType(final String s) {
            return delegate.getMimeType(s);
        }

        @Override
        public Set<String> getResourcePaths(final String s) {
            return delegate.getResourcePaths(s);
        }

        @Override
        public URL getResource(final String s) throws MalformedURLException {
            return delegate.getResource(s);
        }

        @Override
        public InputStream getResourceAsStream(final String s) {
            return delegate.getResourceAsStream(s);
        }

        @Override
        public jakarta.servlet.RequestDispatcher getRequestDispatcher(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.RequestDispatcher getNamedDispatcher(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.Servlet getServlet(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<jakarta.servlet.Servlet> getServlets() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<String> getServletNames() {
            return delegate.getServletNames();
        }

        @Override
        public void log(final String s) {
            delegate.log(s);
        }

        @Override
        public void log(final Exception e, final String s) {
            delegate.log(e, s);
        }

        @Override
        public void log(final String s, final Throwable throwable) {
            delegate.log(s, throwable);
        }

        @Override
        public String getRealPath(final String s) {
            return delegate.getRealPath(s);
        }

        @Override
        public String getServerInfo() {
            return delegate.getServerInfo();
        }

        @Override
        public String getInitParameter(final String s) {
            return delegate.getInitParameter(s);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return delegate.getInitParameterNames();
        }

        @Override
        public boolean setInitParameter(final String s, final String s1) {
            return delegate.setInitParameter(s, s1);
        }

        @Override
        public Object getAttribute(final String s) {
            return delegate.getAttribute(s);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return delegate.getAttributeNames();
        }

        @Override
        public void setAttribute(final String s, final Object o) {
            delegate.setAttribute(s, o);
        }

        @Override
        public void removeAttribute(final String s) {
            delegate.removeAttribute(s);
        }

        @Override
        public String getServletContextName() {
            return delegate.getServletContextName();
        }

        @Override
        public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String s, final String s1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.ServletRegistration getServletRegistration(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, ? extends jakarta.servlet.ServletRegistration> getServletRegistrations() {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.FilterRegistration.Dynamic addFilter(final String s, final String s1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.FilterRegistration getFilterRegistration(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, ? extends jakarta.servlet.FilterRegistration> getFilterRegistrations() {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.SessionCookieConfig getSessionCookieConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<jakarta.servlet.SessionTrackingMode> getDefaultSessionTrackingModes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<jakarta.servlet.SessionTrackingMode> getEffectiveSessionTrackingModes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends EventListener> void addListener(final T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(final Class<? extends EventListener> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends EventListener> T createListener(final Class<T> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClassLoader getClassLoader() {
            return delegate.getClassLoader();
        }

        @Override
        public void declareRoles(final String... strings) {
            delegate.declareRoles(strings);
        }

        @Override
        public String getVirtualServerName() {
            return delegate.getVirtualServerName();
        }

        @Override
        public ServletRegistration.Dynamic addServlet(final String s, final Servlet servlet) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServletRegistration.Dynamic addServlet(final String s, final Class<? extends Servlet> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServletRegistration.Dynamic addJspFile(final String s, final String s1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Servlet> T createServlet(final Class<T> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FilterRegistration.Dynamic addFilter(final String s, final Filter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FilterRegistration.Dynamic addFilter(final String s, final Class<? extends Filter> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Filter> T createFilter(final Class<T> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSessionTrackingModes(final Set<SessionTrackingMode> set) {
            delegate.setSessionTrackingModes(set.stream().map(SessionTrackingMode::name).map(javax.servlet.SessionTrackingMode::valueOf).collect(toSet()));
        }

        @Override
        public int getSessionTimeout() {
            return 0;
        }

        @Override
        public void setSessionTimeout(final int i) {
            // no-op
        }

        @Override
        public String getRequestCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public void setRequestCharacterEncoding(final String s) {
            // no-op
        }

        @Override
        public String getResponseCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public void setResponseCharacterEncoding(final String s) {
            // no-op
        }
    }

    private static class BridgePart implements jakarta.servlet.http.Part {
        private final javax.servlet.http.Part part;

        private BridgePart(final javax.servlet.http.Part part) {
            this.part = part;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return part.getInputStream();
        }

        @Override
        public String getContentType() {
            return part.getContentType();
        }

        @Override
        public String getName() {
            return part.getName();
        }

        @Override
        public String getSubmittedFileName() {
            return part.getSubmittedFileName();
        }

        @Override
        public long getSize() {
            return part.getSize();
        }

        @Override
        public void write(final String s) throws IOException {
            part.write(s);
        }

        @Override
        public void delete() throws IOException {
            part.delete();
        }

        @Override
        public String getHeader(final String s) {
            return part.getHeader(s);
        }

        @Override
        public Collection<String> getHeaders(final String s) {
            return part.getHeaders(s);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return part.getHeaderNames();
        }
    }

    private static class BridgeRequest implements jakarta.servlet.http.HttpServletRequest {
        private final HttpServletRequest delegate;
        private final jakarta.servlet.ServletContext context;

        private BridgeRequest(final HttpServletRequest req, final jakarta.servlet.ServletContext contextBridge) {
            this.delegate = req;
            this.context = contextBridge;
        }

        @Override
        public String getAuthType() {
            return delegate.getAuthType();
        }

        @Override
        public Cookie[] getCookies() {
            final var cookies = delegate.getCookies();
            return cookies == null ? null : Stream.of(cookies).map(it -> {
                final var cookie = new Cookie(it.getName(), it.getName());
                cookie.setComment(it.getComment());
                if (it.getDomain() != null) {
                    cookie.setDomain(it.getDomain());
                }
                cookie.setMaxAge(it.getMaxAge());
                cookie.setPath(it.getPath());
                cookie.setHttpOnly(it.isHttpOnly());
                cookie.setSecure(it.getSecure());
                cookie.setVersion(it.getVersion());
                return cookie;
            }).toArray(Cookie[]::new);
        }

        @Override
        public long getDateHeader(final String s) {
            return delegate.getDateHeader(s);
        }

        @Override
        public String getHeader(final String s) {
            return delegate.getHeader(s);
        }

        @Override
        public Enumeration<String> getHeaders(final String s) {
            return delegate.getHeaders(s);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return delegate.getHeaderNames();
        }

        @Override
        public int getIntHeader(final String s) {
            return delegate.getIntHeader(s);
        }

        @Override
        public String getMethod() {
            return delegate.getMethod();
        }

        @Override
        public String getPathInfo() {
            return delegate.getPathInfo();
        }

        @Override
        public String getPathTranslated() {
            return delegate.getPathTranslated();
        }

        @Override
        public String getContextPath() {
            return delegate.getContextPath();
        }

        @Override
        public String getQueryString() {
            return delegate.getQueryString();
        }

        @Override
        public String getRemoteUser() {
            return delegate.getRemoteUser();
        }

        @Override
        public boolean isUserInRole(final String s) {
            return delegate.isUserInRole(s);
        }

        @Override
        public Principal getUserPrincipal() {
            return delegate.getUserPrincipal();
        }

        @Override
        public String getRequestedSessionId() {
            return delegate.getRequestedSessionId();
        }

        @Override
        public String getRequestURI() {
            return delegate.getRequestURI();
        }

        @Override
        public StringBuffer getRequestURL() {
            return delegate.getRequestURL();
        }

        @Override
        public String getServletPath() {
            return delegate.getServletPath();
        }

        @Override
        public jakarta.servlet.http.HttpSession getSession(final boolean b) {
            final var session = delegate.getSession(b);
            return session == null ? null : new BridgeSession(session, context);
        }

        @Override
        public HttpSession getSession() {
            final var session = delegate.getSession();
            return session == null ? null : new BridgeSession(session, context);
        }

        @Override
        public String changeSessionId() {
            return delegate.changeSessionId();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return delegate.isRequestedSessionIdValid();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return delegate.isRequestedSessionIdFromCookie();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return delegate.isRequestedSessionIdFromURL();
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return delegate.isRequestedSessionIdFromUrl();
        }

        @Override
        public boolean authenticate(final jakarta.servlet.http.HttpServletResponse httpServletResponse) {
            throw new UnsupportedOperationException();
        }


        @Override
        public void login(final String s, final String s1) throws jakarta.servlet.ServletException {
            try {
                delegate.login(s, s1);
            } catch (final ServletException e) {
                throw new jakarta.servlet.ServletException(e.getMessage(), e.getRootCause());
            }
        }

        @Override
        public void logout() throws jakarta.servlet.ServletException {
            try {
                delegate.logout();
            } catch (final ServletException e) {
                throw new jakarta.servlet.ServletException(e.getMessage(), e.getRootCause());
            }
        }

        @Override
        public Collection<jakarta.servlet.http.Part> getParts() throws IOException, jakarta.servlet.ServletException {
            try {
                final var parts = delegate.getParts();
                return parts == null ? null : parts.stream().map(BridgePart::new).collect(toList());
            } catch (final ServletException e) {
                throw new jakarta.servlet.ServletException(e.getMessage(), e.getRootCause());
            }
        }

        @Override
        public Part getPart(final String s) throws IOException, jakarta.servlet.ServletException {
            try {
                final var part = delegate.getPart(s);
                return part == null ? null : new BridgePart(part);
            } catch (final ServletException e) {
                throw new jakarta.servlet.ServletException(e.getMessage(), e.getRootCause());
            }
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(final Class<T> aClass) throws IOException, jakarta.servlet.ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(final String s) {
            return delegate.getAttribute(s);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return delegate.getAttributeNames();
        }

        @Override
        public String getCharacterEncoding() {
            return delegate.getCharacterEncoding();
        }

        @Override
        public void setCharacterEncoding(final String s) throws UnsupportedEncodingException {
            delegate.setCharacterEncoding(s);
        }

        @Override
        public int getContentLength() {
            return delegate.getContentLength();
        }

        @Override
        public long getContentLengthLong() {
            return delegate.getContentLengthLong();
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new BridgeServletInputStream(delegate.getInputStream());
        }

        @Override
        public String getParameter(final String s) {
            return delegate.getParameter(s);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return delegate.getParameterNames();
        }

        @Override
        public String[] getParameterValues(final String s) {
            return delegate.getParameterValues(s);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return delegate.getParameterMap();
        }

        @Override
        public String getProtocol() {
            return delegate.getProtocol();
        }

        @Override
        public String getScheme() {
            return delegate.getScheme();
        }

        @Override
        public String getServerName() {
            return delegate.getServerName();
        }

        @Override
        public int getServerPort() {
            return delegate.getServerPort();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return delegate.getReader();
        }

        @Override
        public String getRemoteAddr() {
            return delegate.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return delegate.getRemoteHost();
        }

        @Override
        public void setAttribute(final String s, final Object o) {
            delegate.setAttribute(s, o);
        }

        @Override
        public void removeAttribute(final String s) {
            delegate.removeAttribute(s);
        }

        @Override
        public Locale getLocale() {
            return delegate.getLocale();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return delegate.getLocales();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public RequestDispatcher getRequestDispatcher(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRealPath(final String s) {
            return delegate.getRealPath(s);
        }

        @Override
        public int getRemotePort() {
            return delegate.getRemotePort();
        }

        @Override
        public String getLocalName() {
            return delegate.getLocalName();
        }

        @Override
        public String getLocalAddr() {
            return delegate.getLocalAddr();
        }

        @Override
        public int getLocalPort() {
            return delegate.getLocalPort();
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return context;
        }

        @Override
        public jakarta.servlet.AsyncContext startAsync() throws IllegalStateException {
            return new BridgeAsyncContext(delegate.startAsync(), context);
        }

        @Override
        public AsyncContext startAsync(final jakarta.servlet.ServletRequest servletRequest, final jakarta.servlet.ServletResponse servletResponse) throws IllegalStateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncStarted() {
            return delegate.isAsyncStarted();
        }

        @Override
        public boolean isAsyncSupported() {
            return delegate.isAsyncSupported();
        }

        @Override
        public AsyncContext getAsyncContext() {
            final var asyncContext = delegate.getAsyncContext();
            return asyncContext == null ? null : new BridgeAsyncContext(asyncContext, context);
        }

        @Override
        public DispatcherType getDispatcherType() {
            final var dispatcherType = delegate.getDispatcherType();
            return dispatcherType == null ? null : DispatcherType.valueOf(dispatcherType.name());
        }
    }

    private static class BridgeAsyncContext implements jakarta.servlet.AsyncContext {
        private final javax.servlet.AsyncContext delegate;
        private final jakarta.servlet.ServletContext context;

        private BridgeAsyncContext(final javax.servlet.AsyncContext delegate, final jakarta.servlet.ServletContext context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public jakarta.servlet.ServletRequest getRequest() {
            return new BridgeRequest(HttpServletRequest.class.cast(delegate.getRequest()), context);
        }

        @Override
        public jakarta.servlet.ServletResponse getResponse() {
            return new BridgeResponse(HttpServletResponse.class.cast(delegate.getRequest()));
        }

        @Override
        public boolean hasOriginalRequestAndResponse() {
            return delegate.hasOriginalRequestAndResponse();
        }

        @Override
        public void dispatch() {
            delegate.dispatch();
        }

        @Override
        public void dispatch(final String s) {
            delegate.dispatch(s);
        }

        @Override
        public void dispatch(final jakarta.servlet.ServletContext servletContext, String s) {
            delegate.dispatch(s);
        }

        @Override
        public void complete() {
            delegate.complete();
        }

        @Override
        public void start(final Runnable runnable) {
            delegate.start(runnable);
        }

        @Override
        public void addListener(final AsyncListener asyncListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(final AsyncListener asyncListener, final jakarta.servlet.ServletRequest servletRequest, final jakarta.servlet.ServletResponse servletResponse) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends AsyncListener> T createListener(final Class<T> aClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTimeout(final long l) {
            delegate.setTimeout(l);
        }

        @Override
        public long getTimeout() {
            return delegate.getTimeout();
        }
    }

    private static class BridgeServletInputStream extends jakarta.servlet.ServletInputStream {
        private final javax.servlet.ServletInputStream delegate;

        private BridgeServletInputStream(final javax.servlet.ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int readLine(final byte[] b, final int off, final int len) throws IOException {
            return delegate.readLine(b, off, len);
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            delegate.setReadListener(new javax.servlet.ReadListener() {
                @Override
                public void onDataAvailable() throws IOException {
                    readListener.onDataAvailable();
                }

                @Override
                public void onAllDataRead() throws IOException {
                    readListener.onAllDataRead();
                }

                @Override
                public void onError(final Throwable throwable) {
                    readListener.onError(throwable);
                }
            });
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return delegate.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            return delegate.readAllBytes();
        }

        @Override
        public byte[] readNBytes(final int len) throws IOException {
            return delegate.readNBytes(len);
        }

        @Override
        public int readNBytes(final byte[] b, final int off, final int len) throws IOException {
            return delegate.readNBytes(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public void mark(int readlimit) {
            delegate.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }

        @Override
        public long transferTo(final OutputStream out) throws IOException {
            return delegate.transferTo(out);
        }
    }

    private static class BridgeServletOutputStream extends jakarta.servlet.ServletOutputStream {
        private final javax.servlet.ServletOutputStream delegate;

        private BridgeServletOutputStream(final javax.servlet.ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void print(final String s) throws IOException {
            delegate.print(s);
        }

        @Override
        public void print(final boolean b) throws IOException {
            delegate.print(b);
        }

        @Override
        public void print(final char c) throws IOException {
            delegate.print(c);
        }

        @Override
        public void print(final int i) throws IOException {
            delegate.print(i);
        }

        @Override
        public void print(final long l) throws IOException {
            delegate.print(l);
        }

        @Override
        public void print(final float f) throws IOException {
            delegate.print(f);
        }

        @Override
        public void print(final double d) throws IOException {
            delegate.print(d);
        }

        @Override
        public void println() throws IOException {
            delegate.println();
        }

        @Override
        public void println(final String s) throws IOException {
            delegate.println(s);
        }

        @Override
        public void println(final boolean b) throws IOException {
            delegate.println(b);
        }

        @Override
        public void println(final char c) throws IOException {
            delegate.println(c);
        }

        @Override
        public void println(final int i) throws IOException {
            delegate.println(i);
        }

        @Override
        public void println(final long l) throws IOException {
            delegate.println(l);
        }

        @Override
        public void println(final float f) throws IOException {
            delegate.println(f);
        }

        @Override
        public void println(final double d) throws IOException {
            delegate.println(d);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(final WriteListener writeListener) {
            delegate.setWriteListener(new javax.servlet.WriteListener() {
                @Override
                public void onWritePossible() throws IOException {
                    writeListener.onWritePossible();
                }

                @Override
                public void onError(final Throwable throwable) {
                    writeListener.onError(throwable);
                }
            });
        }

        @Override
        public void write(final int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static class BridgeResponse implements jakarta.servlet.http.HttpServletResponse {
        private final HttpServletResponse delegate;

        private BridgeResponse(final HttpServletResponse req) {
            this.delegate = req;
        }

        @Override
        public void addCookie(final Cookie it) {
            final var cookie = new javax.servlet.http.Cookie(it.getName(), it.getName());
            cookie.setComment(it.getComment());
            if (it.getDomain() != null) {
                cookie.setDomain(it.getDomain());
            }
            cookie.setMaxAge(it.getMaxAge());
            cookie.setPath(it.getPath());
            cookie.setHttpOnly(it.isHttpOnly());
            cookie.setSecure(it.getSecure());
            cookie.setVersion(it.getVersion());
            delegate.addCookie(cookie);
        }

        @Override
        public boolean containsHeader(final String s) {
            return delegate.containsHeader(s);
        }

        @Override
        public String encodeURL(final String s) {
            return delegate.encodeURL(s);
        }

        @Override
        public String encodeRedirectURL(final String s) {
            return delegate.encodeRedirectURL(s);
        }

        @Override
        public String encodeUrl(final String s) {
            return delegate.encodeUrl(s);
        }

        @Override
        public String encodeRedirectUrl(final String s) {
            return delegate.encodeRedirectUrl(s);
        }

        @Override
        public void sendError(final int i, final String s) throws IOException {
            delegate.sendError(i, s);
        }

        @Override
        public void sendError(final int i) throws IOException {
            delegate.sendError(i);
        }

        @Override
        public void sendRedirect(final String s) throws IOException {
            delegate.sendRedirect(s);
        }

        @Override
        public void setDateHeader(final String s, final long l) {
            delegate.setDateHeader(s, l);
        }

        @Override
        public void addDateHeader(final String s, final long l) {
            delegate.addDateHeader(s, l);
        }

        @Override
        public void setHeader(final String s, final String s1) {
            delegate.setHeader(s, s1);
        }

        @Override
        public void addHeader(final String s, final String s1) {
            delegate.addHeader(s, s1);
        }

        @Override
        public void setIntHeader(final String s, final int i) {
            delegate.setIntHeader(s, i);
        }

        @Override
        public void addIntHeader(final String s, final int i) {
            delegate.addIntHeader(s, i);
        }

        @Override
        public void setStatus(final int i) {
            delegate.setStatus(i);
        }

        @Override
        public void setStatus(final int i, final String s) {
            delegate.setStatus(i, s);
        }

        @Override
        public int getStatus() {
            return delegate.getStatus();
        }

        @Override
        public String getHeader(final String s) {
            return delegate.getHeader(s);
        }

        @Override
        public Collection<String> getHeaders(final String s) {
            return delegate.getHeaders(s);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return delegate.getHeaderNames();
        }

        @Override
        public String getCharacterEncoding() {
            return delegate.getCharacterEncoding();
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new BridgeServletOutputStream(delegate.getOutputStream());
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return delegate.getWriter();
        }

        @Override
        public void setCharacterEncoding(final String s) {
            delegate.setCharacterEncoding(s);
        }

        @Override
        public void setContentLength(final int i) {
            delegate.setContentLength(i);
        }

        @Override
        public void setContentLengthLong(final long l) {
            delegate.setContentLengthLong(l);
        }

        @Override
        public void setContentType(final String s) {
            delegate.setContentType(s);
        }

        @Override
        public void setBufferSize(final int i) {
            delegate.setBufferSize(i);
        }

        @Override
        public int getBufferSize() {
            return delegate.getBufferSize();
        }

        @Override
        public void flushBuffer() throws IOException {
            delegate.flushBuffer();
        }

        @Override
        public void resetBuffer() {
            delegate.resetBuffer();
        }

        @Override
        public boolean isCommitted() {
            return delegate.isCommitted();
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public void setLocale(final Locale locale) {
            delegate.setLocale(locale);
        }

        @Override
        public Locale getLocale() {
            return delegate.getLocale();
        }
    }
}

