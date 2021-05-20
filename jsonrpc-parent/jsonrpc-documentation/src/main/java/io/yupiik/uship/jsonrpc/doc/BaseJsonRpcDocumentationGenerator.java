package io.yupiik.uship.jsonrpc.doc;

import io.yupiik.uship.backbone.reflect.Reflections;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcError;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import io.yupiik.uship.jsonrpc.core.impl.Registration;

import java.io.PrintStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public abstract class BaseJsonRpcDocumentationGenerator implements Runnable {
    private final Collection<Class<?>> endpoints;
    private final PrintStream output;

    public BaseJsonRpcDocumentationGenerator(final Collection<Class<?>> endpoints,
                                             final PrintStream output) {
        this.endpoints = endpoints;
        this.output = output;
    }

    @Override
    public void run() {
        doRun(forRegistrations(), output);
    }

    protected String toString(final Registration registration) {
        return registration.toString();
    }

    protected String asString(final Type type) {
        return type.getTypeName().replace("java.lang.", "").replace("java.util.", "");
    }

    protected void doRun(final Stream<Registration> forRegistrations, final PrintStream output) {
        output.println(forRegistrations.map(this::toString).sorted().collect(joining("\n")));
    }

    private Stream<Registration> forRegistrations() {
        return endpoints.stream().flatMap(this::toRegistration);
    }

    private Stream<Registration> toRegistration(final Class<?> aClass) {
        final var list = Stream.of(aClass.getMethods())
                .filter(it -> it.isAnnotationPresent(JsonRpcMethod.class))
                .map(method -> {
                    final var idx = new AtomicInteger(-1);
                    final var config = of(method.getAnnotation(JsonRpcMethod.class));
                    return new Registration(
                            aClass, method,
                            config.map(JsonRpcMethod::name)
                                    .orElse(method.getDeclaringClass().getName() + "." + method.getName()),
                            Reflections.extractRealType(aClass, method.getGenericReturnType()),
                            a -> null,
                            Stream.of(method.getParameters())
                                    .map(p -> {
                                        final var conf = ofNullable(p.getAnnotation(JsonRpcParam.class));
                                        idx.incrementAndGet();
                                        return new Registration.Parameter(
                                                Reflections.resolveType(Reflections.extractRealType(aClass, p.getParameterizedType()), aClass),
                                                conf.map(JsonRpcParam::value).filter(it -> !it.isEmpty())
                                                        .orElseGet(() -> p.getName() /*don't use method ref, bug in jdk 11.0.2*/),
                                                idx.get(),
                                                conf.map(JsonRpcParam::required).orElse(false),
                                                conf.map(JsonRpcParam::documentation).orElse(""));
                                    })
                                    .collect(toList()),
                            ofNullable(method.getAnnotationsByType(JsonRpcError.class))
                                    .map(ex -> Stream.of(ex)
                                            .map(e -> new Registration.ExceptionMapping(asList(e.handled()), e.code(), e.documentation()))
                                            .collect(toList()))
                                    .orElseGet(Collections::emptyList),
                            config.map(JsonRpcMethod::documentation).orElse(""));
                })
                .collect(toList());
        // remove overriden methods
        final var dropped = list.stream()
                .filter(m -> list.stream()
                        .anyMatch(it -> it.jsonRpcMethod().equals(m.jsonRpcMethod()) &&
                                m != it &&
                                m.method().getDeclaringClass().isAssignableFrom(it.method().getDeclaringClass())))
                .collect(toList());
        list.removeAll(dropped);
        return list.stream();
    }
}
