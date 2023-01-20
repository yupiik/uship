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
package io.yupiik.uship.jsonrpc.cli.test;

import io.yupiik.uship.jsonrpc.cli.api.StdErr;
import io.yupiik.uship.jsonrpc.cli.api.StdOut;
import io.yupiik.uship.jsonrpc.cli.internal.DefaultStreamsProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.function.Supplier;

@Specializes
@ApplicationScoped
public class OverridenStreamsProducer extends DefaultStreamsProducer {
    private final ThreadLocal<Streams> streams = new ThreadLocal<>();

    public ThreadLocal<Streams> getStreams() {
        return streams;
    }

    @Produces
    @StdErr
    @Specializes
    public PrintStream stderr() {
        return stream(() -> System.err, Streams::getStderr);
    }

    @Produces
    @StdOut
    @Specializes
    public PrintStream stdout() {
        return stream(() -> System.out, Streams::getStdout);
    }

    private PrintStream stream(final Supplier<PrintStream> defaultValue, final Function<Streams, PrintStream> accessor) {
        final var forced = streams.get();
        if (forced == null) {
            streams.remove();
            return defaultValue.get();
        }
        return accessor.apply(forced);
    }
}
