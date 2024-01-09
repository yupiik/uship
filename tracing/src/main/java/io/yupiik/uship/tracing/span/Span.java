/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.tracing.span;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

import java.util.Map;

@JsonbPropertyOrder({
        "traceId",
        "parentId",
        "id",
        "name",
        "kind",
        "timestamp",
        "duration",
        "localEndpoint",
        "remoteEndpoint",
        "tags"
})
public class Span {
    private Object traceId;
    private Object parentId;
    private Object id;
    private String name;
    private String kind;
    private Long timestamp;
    private Long duration;
    private Endpoint localEndpoint;
    private Endpoint remoteEndpoint;
    private Map<String, Object> tags;

    public Object getTraceId() {
        return traceId;
    }

    public Span setTraceId(final Object traceId) {
        this.traceId = traceId;
        return this;
    }

    public Object getParentId() {
        return parentId;
    }

    public Span setParentId(final Object parentId) {
        this.parentId = parentId;
        return this;
    }

    public Object getId() {
        return id;
    }

    public Span setId(final Object id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Span setName(final String name) {
        this.name = name;
        return this;
    }

    public String getKind() {
        return kind;
    }

    public Span setKind(final String kind) {
        this.kind = kind;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Span setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public Span setDuration(final Long duration) {
        this.duration = duration;
        return this;
    }

    public Endpoint getLocalEndpoint() {
        return localEndpoint;
    }

    public Span setLocalEndpoint(final Endpoint localEndpoint) {
        this.localEndpoint = localEndpoint;
        return this;
    }

    public Endpoint getRemoteEndpoint() {
        return remoteEndpoint;
    }

    public Span setRemoteEndpoint(final Endpoint remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
        return this;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public Span setTags(final Map<String, Object> tags) {
        this.tags = tags;
        return this;
    }

    @JsonbPropertyOrder({
            "serviceName",
            "ipv4",
            "ipv6",
            "port"
    })
    public static class Endpoint {
        private String serviceName;
        private String ipv4;
        private String ipv6;
        private int port;

        public String getServiceName() {
            return serviceName;
        }

        public Endpoint setServiceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public String getIpv4() {
            return ipv4;
        }

        public Endpoint setIpv4(final String ipv4) {
            this.ipv4 = ipv4;
            return this;
        }

        public String getIpv6() {
            return ipv6;
        }

        public Endpoint setIpv6(final String ipv6) {
            this.ipv6 = ipv6;
            return this;
        }

        public int getPort() {
            return port;
        }

        public Endpoint setPort(final int port) {
            this.port = port;
            return this;
        }
    }

    public static class Annotation {
        private String key;
        private int type;
        private Object value;

        public String getKey() {
            return key;
        }

        public Annotation setKey(final String key) {
            this.key = key;
            return this;
        }

        public int getType() {
            return type;
        }

        public Annotation setType(final int type) {
            this.type = type;
            return this;
        }

        public Object getValue() {
            return value;
        }

        public Annotation setValue(final Object value) {
            this.value = value;
            return this;
        }
    }
}
