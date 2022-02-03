/*
 * Copyright (c) 2021, 2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.core.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Function;

public class Registration {
    private final Class<?> clazz;
    private final Method method;
    private final String jsonRpcMethod;
    private final Type returnedType;
    private final Function<Object[], Object> invoker;
    private final Collection<Parameter> parameters;
    private final Collection<ExceptionMapping> exceptionMappings;
    private final String documentation;

    public Registration(final Class<?> clazz, final Method method, final String jsonRpcMethod, final Type returnedType,
                        final Function<Object[], Object> invoker, final Collection<Parameter> parameters,
                        final Collection<ExceptionMapping> exceptionMappings, final String documentation) {
        this.clazz = clazz;
        this.method = method;
        this.jsonRpcMethod = jsonRpcMethod;
        this.returnedType = returnedType;
        this.invoker = invoker;
        this.parameters = parameters;
        this.exceptionMappings = exceptionMappings;
        this.documentation = documentation;
    }

    public Class<?> clazz() {
        return clazz;
    }

    public Method method() {
        return method;
    }

    public String jsonRpcMethod() {
        return jsonRpcMethod;
    }

    public Type returnedType() {
        return returnedType;
    }

    public Function<Object[], Object> invoker() {
        return invoker;
    }

    public Collection<Parameter> parameters() {
        return parameters;
    }

    public Collection<ExceptionMapping> exceptionMappings() {
        return exceptionMappings;
    }

    public String documentation() {
        return documentation;
    }

    public static class ExceptionMapping {
        private final Collection<Class<? extends Throwable>> types;
        private final int code;
        private final String documentation;

        public ExceptionMapping(final Collection<Class<? extends Throwable>> types, final int code, final String documentation) {
            this.types = types;
            this.code = code;
            this.documentation = documentation;
        }

        public Collection<Class<? extends Throwable>> types() {
            return types;
        }

        public int code() {
            return code;
        }

        public String documentation() {
            return documentation;
        }
    }

    public static class Parameter {
        private final Type type;
        private final String name;
        private final int position;
        private final boolean required;
        private final String documentation;

        public Parameter(final Type type, final String name, final int position, final boolean required, final String documentation) {
            this.type = type;
            this.name = name;
            this.position = position;
            this.required = required;
            this.documentation = documentation;
        }

        public Type type() {
            return type;
        }

        public String name() {
            return name;
        }

        public int position() {
            return position;
        }

        public boolean required() {
            return required;
        }

        public String documentation() {
            return documentation;
        }
    }
}
