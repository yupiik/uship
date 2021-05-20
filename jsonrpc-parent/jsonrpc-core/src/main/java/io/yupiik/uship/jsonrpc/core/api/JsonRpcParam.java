package io.yupiik.uship.jsonrpc.core.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
public @interface JsonRpcParam {
    String value() default "";
    boolean required() default false;
    String documentation() default "";
}
