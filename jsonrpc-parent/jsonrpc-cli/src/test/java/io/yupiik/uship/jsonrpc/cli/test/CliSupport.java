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
package io.yupiik.uship.jsonrpc.cli.test;

import io.yupiik.uship.jsonrpc.cli.api.StdOut;
import io.yupiik.uship.jsonrpc.cli.main.JsonRpcCli;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ExtendWith(CliSupport.CliExtension.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface CliSupport {
    ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CliSupport.class.getName());

    class CliExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
        private static JsonRpcCli cli;
        private static SeContainer container;

        private static final ResettableStream STDOUT = new ResettableStream();
        private static final ResettableStream STDERR = new ResettableStream();

        private final Collection<CreationalContext<Object>> creationalContexts = new ArrayList<>();

        @Override
        public void beforeAll(final ExtensionContext extensionContext) {
            extensionContext.getStore(NAMESPACE).put(Streams.class, new Streams(STDOUT, STDERR));
            if (cli != null) {
                return;
            }

            Locale.setDefault(Locale.ENGLISH);
            cli = new JsonRpcCli();
            container = cli.createContainer();
            container.select(OverridenStreamsProducer.class).get().getStreams().set(new Streams(STDOUT, STDERR));
            Runtime.getRuntime().addShutdownHook(new Thread(
                    () -> container.close(), getClass().getName() + "-shutdown"));
        }

        @Override
        public void beforeEach(final ExtensionContext extensionContext) {
            STDOUT.reset();
            STDERR.reset();
            extensionContext.getTestInstances().ifPresent(testInstances ->
                    testInstances.getAllInstances().stream().distinct().forEach(instance -> {
                        final BeanManager manager = container.getBeanManager();
                        final AnnotatedType<?> annotatedType = manager.createAnnotatedType(instance.getClass());
                        final InjectionTarget injectionTarget = manager.createInjectionTarget(annotatedType);
                        final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
                        creationalContexts.add(creationalContext);
                        injectionTarget.inject(instance, creationalContext);
                    }));
        }

        @Override
        public void afterEach(final ExtensionContext extensionContext) {
            if (!creationalContexts.isEmpty()) {
                creationalContexts.forEach(CreationalContext::release);
                creationalContexts.clear();
            }
        }

        @Override
        public boolean supportsParameter(final ParameterContext parameterContext,
                                         final ExtensionContext extensionContext) throws ParameterResolutionException {
            return ResettableStream.class == parameterContext.getParameter().getType();
        }

        @Override
        public Object resolveParameter(final ParameterContext parameterContext,
                                       final ExtensionContext extensionContext) throws ParameterResolutionException {
            return parameterContext.getParameter().isAnnotationPresent(StdOut.class) ? STDOUT : STDERR;
        }
    }

    class ResettableStream extends PrintStream {
        private ResettableStream() {
            super(new ByteArrayOutputStream());
        }

        public String asString() {
            return ((ByteArrayOutputStream) out).toString(StandardCharsets.UTF_8);
        }

        private void reset() {
            ByteArrayOutputStream.class.cast(out).reset();
        }
    }
}
