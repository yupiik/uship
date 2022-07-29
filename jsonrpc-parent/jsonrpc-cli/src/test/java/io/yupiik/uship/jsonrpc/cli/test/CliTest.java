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

import io.yupiik.uship.jsonrpc.cli.api.JsonRpcCliExecutor;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

@TestTemplate
@Target(METHOD)
@Retention(RUNTIME)
@ExtendWith(CliTest.CliTestExtension.class)
public @interface CliTest {
    String IGNORE_STREAM = "<io.yupiik.uship.jsonrpc.cli.test.CliTest.ignore>";

    String stdout() default "";

    String stderr() default "";

    String[] command() default "";

    class CliTestExtension implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(final ExtensionContext context) {
            return isAnnotated(context.getElement(), CliTest.class);
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
            return context
                    .getElement()
                    .flatMap(e -> findAnnotation(e, CliTest.class)).stream()
                    .map(conf -> toTest(context, conf));
        }

        private TestTemplateInvocationContext toTest(final ExtensionContext context, final CliTest config) {
            return new CliTestImpl(config, context.getStore(CliSupport.NAMESPACE).get(Streams.class, Streams.class));
        }

        private static class CliTestImpl implements TestTemplateInvocationContext {
            private final CliTest config;
            private final Streams streams;

            private CliTestImpl(final CliTest config, final Streams streams) {
                this.config = config;
                this.streams = streams;
            }

            @Override
            public String getDisplayName(final int invocationIndex) {
                return '[' + String.join(" ", config.command()) + ']';
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return singletonList(new CliTestRunner(config, streams));
            }
        }

        private static class CliTestRunner implements InvocationInterceptor {
            private final CliTest config;
            private final Streams streams;

            private CliTestRunner(final CliTest config, final Streams streams) {
                this.config = config;
                this.streams = streams;
            }

            @Override
            public void interceptTestTemplateMethod(final Invocation<Void> invocation,
                                                    final ReflectiveInvocationContext<Method> invocationContext,
                                                    final ExtensionContext extensionContext) throws Throwable {
                try {
                    CDI.current().select(JsonRpcCliExecutor.class).get()
                            .execute(config.command())
                            .toCompletableFuture()
                            .get();
                } catch (final ExecutionException e) {
                    // no-op
                }
                postExecute(invocation);
            }

            private void postExecute(final Invocation<Void> invocation) throws Throwable {
                invocation.proceed();
                final String stdout = config.stdout();
                if (!CliTest.IGNORE_STREAM.equals(stdout)) {
                    final String out = streams.getStdout().asString();
                    assertEquals(stdout, out, () -> {
                        final String stderr = streams.getStderr().asString();
                        return "stdout:\n'" + out + "'\n\nstderr:\n'" + stderr + '\'';
                    });
                }
                final String stderr = config.stderr();
                if (!CliTest.IGNORE_STREAM.equals(stderr)) {
                    assertEquals(stderr, streams.getStderr().asString());
                }
            }
        }
    }
}
