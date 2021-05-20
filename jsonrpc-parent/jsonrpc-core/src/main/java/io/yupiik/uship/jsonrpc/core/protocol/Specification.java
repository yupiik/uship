/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.core.protocol;

import java.util.Collection;
import java.util.Map;

public class Specification {
    private Map<String, MethodSpecification> methods;

    public Specification() {
        // no-op
    }

    public Specification(final Map<String, MethodSpecification> methods) {
        this.methods = methods;
    }

    public Map<String, MethodSpecification> getMethods() {
        return methods;
    }

    public static class ExceptionSpecification {
        private int code;
        private String description;

        public ExceptionSpecification() {
            // no-op
        }

        public ExceptionSpecification(final int code, final String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public void setCode(final int code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }
    }

    public static class ParameterSpecification {
        private String name;
        private int position;
        private String description;

        public ParameterSpecification() {
            // no-op
        }

        public ParameterSpecification(final String name, final int position, final String description) {
            this.name = name;
            this.position = position;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(final int position) {
            this.position = position;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }
    }

    public static class MethodSpecification {
        private String method;
        private String documentation;
        private Collection<ParameterSpecification> parameters;
        private Collection<ExceptionSpecification> exceptions;

        public MethodSpecification() {
            // no-op
        }

        public MethodSpecification(final String method, final String documentation,
                                   final Collection<ParameterSpecification> parameters,
                                   final Collection<ExceptionSpecification> exceptions) {
            this.method = method;
            this.documentation = documentation;
            this.parameters = parameters;
            this.exceptions = exceptions;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(final String method) {
            this.method = method;
        }

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(final String documentation) {
            this.documentation = documentation;
        }

        public Collection<ParameterSpecification> getParameters() {
            return parameters;
        }

        public void setParameters(final Collection<ParameterSpecification> parameters) {
            this.parameters = parameters;
        }

        public Collection<ExceptionSpecification> getExceptions() {
            return exceptions;
        }

        public void setExceptions(final Collection<ExceptionSpecification> exceptions) {
            this.exceptions = exceptions;
        }
    }
}
