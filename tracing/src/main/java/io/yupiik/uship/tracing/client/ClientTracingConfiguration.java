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
package io.yupiik.uship.tracing.client;

import java.util.Map;

public class ClientTracingConfiguration {
    private String operation = "httpclient";
    private String serviceName = "httpclient";
    private Map<String, Object> tags = Map.of("component", "yupiik-httpclient");
    private String parentHeader = "X-B3-ParentSpanId";
    private String spanHeader = "X-B3-SpanId";
    private String traceHeader = "X-B3-TraceId";

    public String getServiceName() {
        return serviceName;
    }

    public ClientTracingConfiguration setServiceName(final String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getParentHeader() {
        return parentHeader;
    }

    public ClientTracingConfiguration setParentHeader(final String parentHeader) {
        this.parentHeader = parentHeader;
        return this;
    }

    public String getOperation() {
        return operation;
    }

    public ClientTracingConfiguration setOperation(final String operation) {
        this.operation = operation;
        return this;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public ClientTracingConfiguration setTags(final Map<String, Object> tags) {
        this.tags = tags;
        return this;
    }

    public String getSpanHeader() {
        return spanHeader;
    }

    public ClientTracingConfiguration setSpanHeader(final String spanHeader) {
        this.spanHeader = spanHeader;
        return this;
    }

    public String getTraceHeader() {
        return traceHeader;
    }

    public ClientTracingConfiguration setTraceHeader(final String traceHeader) {
        this.traceHeader = traceHeader;
        return this;
    }
}
