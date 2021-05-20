package io.yupiik.uship.jsonrpc.core.api;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
@Repeatable(JsonRpcError.List.class)
public @interface JsonRpcError {
    Class<? extends Throwable>[] handled() default {};

    int code();

    String documentation() default "";

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface List {
        JsonRpcError[] value();
    }
}
