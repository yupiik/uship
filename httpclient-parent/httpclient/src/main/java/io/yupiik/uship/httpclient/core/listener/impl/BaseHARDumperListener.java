/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.httpclient.core.listener.impl;

import io.yupiik.uship.httpclient.core.listener.RequestListener;
import io.yupiik.uship.httpclient.core.request.UnlockedHttpRequest;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbDateFormat;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static jakarta.json.bind.config.PropertyOrderStrategy.LEXICOGRAPHICAL;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public abstract class BaseHARDumperListener implements RequestListener<BaseHARDumperListener.Data> {
    protected final BaseConfiguration configuration;

    public BaseHARDumperListener(final BaseConfiguration configuration) {
        this.configuration = configuration;
    }

    protected abstract void onEntry(Har.Entry entry);

    protected abstract boolean isJsonFormatted();

    protected Jsonb newJsonb() {
        return JsonbBuilder.create(new JsonbConfig()
                .withFormatting(isJsonFormatted())
                .withPropertyOrderStrategy(LEXICOGRAPHICAL)
                .setProperty("johnzon.skip-cdi", true));
    }

    @Override
    public State<Data> before(final long count, final HttpRequest request) {
        final var requestPayload = readRequestPayload(request);
        if (requestPayload == null) {
            return new State<>(request, new Data(configuration.clock.instant(), null));
        }
        return new State<>(
                new UnlockedHttpRequest(
                        Optional.of(HttpRequest.BodyPublishers.ofByteArray(requestPayload)),
                        request.method(),
                        request.timeout(),
                        request.expectContinue(),
                        request.uri(),
                        request.version(),
                        request.headers()),
                new Data(configuration.clock.instant(), requestPayload));
    }

    @Override
    public void after(final Data state, final HttpRequest request, final Throwable error, final HttpResponse<?> response) {
        final var entry = new Har.Entry();
        entry.request = toRequest(request, state.requestPayload);
        entry.response = toResponse(response);
        onEntry(entry);
    }

    private Har.Request toRequest(final HttpRequest request, final byte[] body) {
        final var req = new Har.Request();

        if (body != null) {
            req.bodySize = body.length;

            final var postData = new Har.PostData();
            request.headers().firstValue("Content-Type").ifPresent(contentType -> {
                postData.mimeType = contentType;
                if (contentType.contains("form")) { // todo: distinguish between multiple flavors since parsing is different
                    postData.params = Stream.of(new String(body, StandardCharsets.UTF_8).split("&"))
                            .map(it -> {
                                final var eq = it.indexOf('=');
                                final var param = new Har.Param();
                                if (eq > 0) {
                                    param.name = it.substring(0, eq);
                                    param.value = it.substring(eq + 1);
                                } else {
                                    param.name = it;
                                    param.value = "";
                                }
                                return param;
                            })
                            .collect(toList());
                } else {
                    postData.text = new String(body, StandardCharsets.UTF_8);
                }
            });
            if (postData.params == null && postData.text == null) {
                postData.text = new String(body, StandardCharsets.UTF_8);
            }
            req.postData = postData;
        } else {
            req.bodySize = -1;
        }

        req.method = request.method();
        req.url = request.uri().toASCIIString();
        req.queryString = ofNullable(request.uri().getQuery())
                .filter(it -> !it.isBlank())
                .map(q -> { // drop it from the url
                    req.url = req.url.replace("?" + q, "");
                    return q;
                })
                .map(query -> Stream.of(query.split("&"))
                        .map(it -> {
                            final int eq = it.indexOf('=');
                            if (eq > 0) {
                                return new String[]{it.substring(0, eq), it.substring(eq + 1)};
                            }
                            return new String[]{it, ""};
                        })
                        .map(arr -> {
                            final var q = new Har.Query();
                            q.name = arr[0];
                            q.value = arr[1];
                            return q;
                        })
                        .collect(toList()))
                .orElse(null);
        req.headers = toHeaders(request.headers());
        req.headerSize = toHeaderSize(req.headers);
        // todo: cookies - but same than for response mapping, not strictly needed yet

        return req;
    }

    private Har.Response toResponse(HttpResponse<?> response) {
        final var harResponse = new Har.Response();

        if (CharSequence.class.isInstance(response.body())) {
            final var body = String.valueOf(response.body()).getBytes(StandardCharsets.UTF_8);
            setContentFromBody(response, harResponse, body);
        } else if (byte[].class.isInstance(response.body())) {
            final var body = byte[].class.cast(response.body());
            setContentFromBody(response, harResponse, body);
        } else if (Path.class.isInstance(response.body())) {
            final var body = Path.class.cast(response.body());
            try {
                setContentFromBody(response, harResponse, Files.readAllBytes(body));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        } else if (response.body() != null) {
            throw new IllegalArgumentException("Unsupported HAR support for body: " + response.body());
        } else {
            harResponse.bodySize = -1;
        }

        final var headers = response.headers();

        harResponse.status = response.statusCode();
        if (harResponse.status == 301 || harResponse.status == 302 || harResponse.status == 303 || harResponse.status == 308) {
            harResponse.redirectURL = headers.firstValue("Location").orElse(null);
        }
        harResponse.headers = toHeaders(headers);
        harResponse.headersSize = toHeaderSize(harResponse.headers);
        // todo: cookies - not strictly required since they should be in headers

        return harResponse;
    }

    private long toHeaderSize(final Collection<Har.Header> headers) {
        return headers.stream()
                .mapToLong(h -> h.name.getBytes(StandardCharsets.UTF_8).length + h.value.getBytes(StandardCharsets.UTF_8).length + ": \r\n".length())
                .sum();
    }

    private Collection<Har.Header> toHeaders(final HttpHeaders headers) {
        return headers == null ? List.of() : headers.map().entrySet().stream()
                .map(it -> {
                    final var header = new Har.Header();
                    header.name = it.getKey();
                    header.value = String.join(",", it.getValue());
                    return header;
                })
                .collect(toList());
    }

    private void setContentFromBody(final HttpResponse<?> response, final Har.Response harResponse, final byte[] body) {
        harResponse.bodySize = body == null ? -1 : body.length;

        final var content = new Har.Content();
        content.size = harResponse.bodySize;
        harResponse.content = content;
        if (harResponse.bodySize >= 0 && response.headers() != null) {
            response.headers().firstValue("Content-Type").ifPresent(contentType -> {
                content.mimeType = contentType;
                if (List.of("application/octet-stream", "multipart/form-data").contains(contentType) ||
                        contentType.startsWith("application/vnd.openxmlformats-officedocument")) {
                    content.encoding = "base64";
                    content.text = Base64.getEncoder().encodeToString(body);
                }
            });
            if (content.text == null) {
                content.text = new String(body, StandardCharsets.UTF_8);
            }
        }
    }

    private byte[] readRequestPayload(final HttpRequest request) {
        return request.bodyPublisher().map(p -> {
            if (p.contentLength() == 0) {
                return null;
            }
            final var subscriber = HttpResponse.BodySubscribers.ofByteArray();
            p.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(final Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(final ByteBuffer item) {
                    subscriber.onNext(List.of(item));
                }

                @Override
                public void onError(final Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
            try {
                return subscriber.getBody().toCompletableFuture().get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (final ExecutionException e) {
                throw new IllegalStateException(e.getCause());
            }
        }).orElse(null);
    }

    protected static class Data {
        private final Instant instant;
        private final byte[] requestPayload;

        protected Data(final Instant instant, final byte[] requestPayload) {
            this.instant = instant;
            this.requestPayload = requestPayload;
        }
    }

    // see http://www.softwareishard.com/blog/har-12-spec/
    // mapping taken from https://github.com/rmannibucau/mock-server-generator
    public static class Har {
        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

        public Log log = new Log();

        public static class Log {
            public String version = "1.2";
            public Creator creator;
            public Browser browser;
            public Collection<Page> pages;
            public Collection<Entry> entries;
            public String comment = "";
        }

        public static class Request {
            public String method;
            public String url;
            public String httpVersion = "HTTP/1.1";
            public Collection<Cookie> cookies;
            public Collection<Header> headers;
            public Collection<Query> queryString;
            public PostData postData;
            public long headerSize = -1;
            public long bodySize;
            public String comment = "";
        }

        public static class Response {
            public int status = 200;
            public String statusText = "OK";
            public String httpVersion = "HTTP/1.1";
            public Collection<Cookie> cookies;
            public Collection<Header> headers;
            public Content content;
            public String redirectURL;
            public long headersSize;
            public long bodySize;
            public String comment = "";
        }

        public static class Query {
            public String name;
            public String value;
            public String comment = "";
        }

        public static class PostData {
            public String mimeType;
            public Collection<Param> params;
            public String text;
            public String comment = "";
        }

        public static class Param {
            public String name;
            public String value;
            public String fileName;
            public String contentType;
            public String comment = "";
        }

        public static class Cache {
            public BeforeRequest beforeRequest;
            public AfterRequest afterRequest;
            public String comment = "";
        }

        public static abstract class CacheRequest {
            @JsonbDateFormat(DATE_FORMAT)
            public ZonedDateTime expires;

            @JsonbDateFormat(DATE_FORMAT)
            public ZonedDateTime lastAccess;

            public String eTag;
            public int hitCount;
            public String comment;
        }

        public static class BeforeRequest extends CacheRequest {
        }

        public static class AfterRequest extends CacheRequest {
        }

        public static class Timings {
            public long blocked = -1;
            public long dns = -1;
            public long connect = -1;
            public long send = 0;
            public long wait = 0;
            public long receive = 0;
            public long ssl = -1;
            public String comment = "";
        }

        public static class Cookie {
            @JsonbDateFormat(DATE_FORMAT)
            public ZonedDateTime expires;
            public String name;
            public String value;
            public String path;
            public String domain;
            public boolean httpOnly;
            public boolean secure;
            public String comment;
        }

        public static class Header {
            public String name;
            public String value;
            public String comment;
        }

        public static class Content {
            public long size;
            public int compression;
            public String mimeType;
            public String text;
            public String encoding; // base64 if text is encoded
            public String comment;
        }

        public static class Entry {
            @JsonbDateFormat(DATE_FORMAT)
            public ZonedDateTime startedDateTime;
            public String pageref;
            public long time;
            public Request request;
            public Response response;
            public Cache cache;
            public Timings timings;
            public String serverIPAddress;
            public String connection;
            public String comment;
        }

        public static class Page {
            @JsonbDateFormat(DATE_FORMAT)
            public ZonedDateTime startedDateTime;
            public String id;
            public String title;
            public PageTiming pageTimings;
            public String comment = "";
        }

        public static class PageTiming {
            public long onContentLoad;
            public long onLoad;
            public String comment;
        }

        public static abstract class BaseIdentity {
            public String name;
            public String version;
            public String comment = "";
        }

        public static class Creator extends BaseIdentity {
        }

        public static class Browser extends BaseIdentity {
        }
    }

    public static class BaseConfiguration {
        protected final Path output;
        protected final Clock clock;
        protected final Logger logger;

        protected BaseConfiguration(final Path output, final Clock clock, final Logger logger) {
            this.output = output;
            this.clock = clock;
            this.logger = logger;
        }
    }
}
