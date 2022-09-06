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

import io.yupiik.uship.httpclient.core.request.UnlockedHttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HARDumperListenerTest {

    @Test
    void harJson(@TempDir final Path temp) throws Exception {
        final var output = temp.resolve("harJson.json");
        final String expected = "{\n" + "  \"log\":{\n" + "    \"comment\":\"\",\n"
            + "    \"entries\":[\n" + "      {\n" + "        \"request\":{\n"
            + "          \"bodySize\":-1,\n" + "          \"comment\":\"\",\n"
            + "          \"headerSize\":0,\n" + "          \"headers\":[\n" + "          ],\n"
            + "          \"httpVersion\":\"HTTP/1.1\",\n" + "          \"method\":\"GET\",\n"
            + "          \"url\":\"http://localhost:1234/test1\"\n" + "        },\n"
            + "        \"response\":{\n" + "          \"bodySize\":0,\n"
            + "          \"comment\":\"\",\n" + "          \"content\":{\n"
            + "            \"compression\":0,\n" + "            \"size\":0\n" + "          },\n"
            + "          \"headers\":[\n" + "          ],\n" + "          \"headersSize\":0,\n"
            + "          \"httpVersion\":\"HTTP/1.1\",\n" + "          \"status\":200,\n"
            + "          \"statusText\":\"OK\"\n" + "        },\n"
            + "        \"startedDateTime\":\"1970-01-01T00:00:00.000Z\",\n" + "        \"time\":1\n"
            + "      }\n" + "    ],\n" + "    \"version\":\"1.2\"\n" + "  }\n" + "}";

        var clock = new Clock() {
            private Instant instant = Instant.EPOCH;
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return instant;
            }

            public void plusMillis(long value) {
                instant = instant.plusMillis(value);
            }
        };
        final HARDumperListener.Configuration configuration = new HARDumperListener.Configuration(
            output,
            clock,
            Logger.getLogger("test"));

        try (final var listener = new HARDumperListener(configuration)) {

            final var request = requestGet();
            final var before = listener.before(1, request);
            clock.plusMillis(1);
            listener.after(before.state(), before.request(), null, response(200, request));
        }

        assertEquals(expected, Files.readString(output));
    }
    @Test
    void harJsonTimeDisable(@TempDir final Path temp) throws Exception {
        final var output = temp.resolve("harJsonTimeDisable.json");
        final String expected = "{\n" + "  \"log\":{\n" + "    \"comment\":\"\",\n"
            + "    \"entries\":[\n" + "      {\n" + "        \"request\":{\n"
            + "          \"bodySize\":-1,\n" + "          \"comment\":\"\",\n"
            + "          \"headerSize\":0,\n" + "          \"headers\":[\n" + "          ],\n"
            + "          \"httpVersion\":\"HTTP/1.1\",\n" + "          \"method\":\"GET\",\n"
            + "          \"url\":\"http://localhost:1234/test1\"\n" + "        },\n"
            + "        \"response\":{\n" + "          \"bodySize\":0,\n"
            + "          \"comment\":\"\",\n" + "          \"content\":{\n"
            + "            \"compression\":0,\n" + "            \"size\":0\n" + "          },\n"
            + "          \"headers\":[\n" + "          ],\n" + "          \"headersSize\":0,\n"
            + "          \"httpVersion\":\"HTTP/1.1\",\n" + "          \"status\":200,\n"
            + "          \"statusText\":\"OK\"\n" + "        },\n"
            + "        \"time\":0\n"
            + "      }\n" + "    ],\n" + "    \"version\":\"1.2\"\n" + "  }\n" + "}";

        var clock = new Clock() {
            private Instant instant = Instant.EPOCH;
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return instant;
            }

            public void plusMillis(long value) {
                instant = instant.plusMillis(value);
            }
        };
        final HARDumperListener.Configuration configuration = new HARDumperListener.Configuration(
            output,
            clock,
            Logger.getLogger("test")).setEnableTime(false).setEnableStartedDateTime(false);

        try (final var listener = new HARDumperListener(configuration)) {

            final var request = requestGet();
            final var before = listener.before(1, request);
            clock.plusMillis(1);
            listener.after(before.state(), before.request(), null, response(200, request));
        }

        assertEquals(expected, Files.readString(output));
    }

    private HttpRequest requestGet() {
        return new UnlockedHttpRequest("GET",
            URI.create("http://localhost:1234/test1"),
            HttpHeaders.of(Map.of(), (a, b) -> true));
    }

    private HttpResponse<String> response(final int status, final HttpRequest request) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public HttpRequest request() {
                return request;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return "";
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return request.uri();
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    @Test
    void configurationTest() {
        var myTestConfig = new TestConfiguration(Path.of("/"), Clock.systemUTC(), Logger.getLogger("test"))
            .setEnableTime(false);
        assertTrue(myTestConfig instanceof TestConfiguration);
        assertEquals(false, myTestConfig.isEnableTime());
        assertEquals(true, myTestConfig.isEnableStartedDateTime());
        myTestConfig = myTestConfig.setFoo("foo").setEnableStartedDateTime(false);
        assertTrue(myTestConfig instanceof TestConfiguration);
        assertEquals("foo", myTestConfig.getFoo());
        assertEquals(false, myTestConfig.isEnableTime());
        assertEquals(false, myTestConfig.isEnableStartedDateTime());
        myTestConfig = myTestConfig.setEnableTime(true).setFoo("bar");
        assertTrue(myTestConfig instanceof TestConfiguration);
        assertEquals("bar", myTestConfig.getFoo());
        assertEquals(true, myTestConfig.isEnableTime());


        var myBaseConfig = new BaseHARDumperListener.BaseConfiguration(Path.of("/"), Clock.systemUTC(), Logger.getLogger("test"));
        assertTrue(myBaseConfig instanceof BaseHARDumperListener.BaseConfiguration);
        assertEquals(true, myBaseConfig.isEnableTime());
        assertEquals(true, myBaseConfig.isEnableStartedDateTime());
        myBaseConfig = myBaseConfig.setEnableTime(false).setEnableStartedDateTime(false);
        assertEquals(false, myBaseConfig.isEnableTime());
        assertEquals(false, myBaseConfig.isEnableStartedDateTime());
        assertTrue(myBaseConfig instanceof BaseHARDumperListener.BaseConfiguration);

    }

    private static final class TestConfiguration extends BaseHARDumperListener.BaseConfiguration<TestConfiguration> {

        private String foo;
        public TestConfiguration(Path output, Clock clock, Logger logger) {
            super(output, clock, logger);
        }

        public String getFoo() {
            return foo;
        }

        public TestConfiguration setFoo(String foo) {
            this.foo = foo;
            return this;
        }
    }

}
