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
package io.yupiik.uship.jsonrpc.cli.main;

import io.yupiik.uship.jsonrpc.cli.api.JsonRpcCliExecutor;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.completedFuture;

public final class JsonRpcCli {
    public static void main(final String... args) {
        final var cli = new JsonRpcCli();
        try (final SeContainer container = cli.createContainer()) {
            final var commandHandler = cli.getCommandHandler(container);
            if (args.length == 2 && ("-f".equalsIgnoreCase(args[0]) || "--file".equalsIgnoreCase(args[0]))) {
                CompletionStage<?> global = completedFuture(null);
                try (final BufferedReader reader = Files.newBufferedReader(Paths.get(args[1]))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String argLine = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        global = global.thenCompose(i -> commandHandler.execute(parse(argLine).toArray(new String[0])));
                    }
                    global.toCompletableFuture().get(); // await
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                commandHandler.execute(args).toCompletableFuture().get();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            System.exit(-1);
        }
    }

    private JsonRpcCliExecutor handler;

    public JsonRpcCliExecutor getCommandHandler(final SeContainer container) {
        return handler == null ? handler = container.select(JsonRpcCliExecutor.class).get() : handler;
    }

    public SeContainer createContainer() {
        setupLogging();
        return SeContainerInitializer.newInstance().initialize();
    }

    private static void setupLogging() {
        setProperty("org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery.level", "SEVERE");
        setProperty("org.apache.webbeans.level", "WARNING");
        setProperty("io.yupiik.uship.jsonrpc.web.level", "WARNING");
        setProperty("io.yupiik.uship.jsonrpc.cli.internal", "WARNING");
    }

    private static void setProperty(final String key, final String value) {
        System.setProperty(key, System.getProperty(key, value));
    }

    public static Collection<String> parse(final String raw) {
        final Collection<String> result = new ArrayList<>();

        Character end = null;
        boolean escaped = false;
        final StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (escaped) {
                escaped = false;
                current.append(c);
            } else if ((end != null && end == c) || (c == ' ' && end == null)) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                end = null;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"' || c == '\'') {
                end = c;
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
