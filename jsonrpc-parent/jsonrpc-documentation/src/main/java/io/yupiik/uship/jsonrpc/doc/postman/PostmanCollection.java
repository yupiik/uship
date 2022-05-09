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
package io.yupiik.uship.jsonrpc.doc.postman;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbProperty;

import java.util.List;

public class PostmanCollection {
    @JsonbProperty("$schema")
    private String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";
    private Info info; // required
    private List<Item> items; // required
    private List<Variable> variable;

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public List<Variable> getVariable() {
        return variable;
    }

    public void setVariable(final List<Variable> variable) {
        this.variable = variable;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(final Info info) {
        this.info = info;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
    }

    public static class Variable {
        private String id;
        private String key;
        private String value;
        private String type;
        private String name;
        private String description;
        private Boolean system;
        private Boolean disabled;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public Boolean isSystem() {
            return system;
        }

        public void setSystem(final Boolean system) {
            this.system = system;
        }

        public Boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }
    }

    public static class Body {
        private String mode = "raw";
        private String raw;
        private String certificate;
        private Proxy proxy;
        private Auth auth;
        private Boolean disabled;
        private JsonObject options;

        public String getMode() {
            return mode;
        }

        public void setMode(final String mode) {
            this.mode = mode;
        }

        public String getRaw() {
            return raw;
        }

        public void setRaw(final String raw) {
            this.raw = raw;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(final String certificate) {
            this.certificate = certificate;
        }

        public Proxy getProxy() {
            return proxy;
        }

        public void setProxy(final Proxy proxy) {
            this.proxy = proxy;
        }

        public Auth getAuth() {
            return auth;
        }

        public void setAuth(final Auth auth) {
            this.auth = auth;
        }

        public Boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }

        public JsonObject getOptions() {
            return options;
        }

        public void setOptions(final JsonObject options) {
            this.options = options;
        }
    }

    public static class Basic {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }
    }

    public static class Bearer {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(final String token) {
            this.token = token;
        }
    }

    public static class Auth {
        private String type = "noauth";
        private JsonObject noauth = JsonValue.EMPTY_JSON_OBJECT;
        private JsonObject apikey;
        private JsonObject awsv4;
        private Basic basic;
        private Bearer bearer;
        private JsonObject digest;
        private JsonObject edgegrid;
        private JsonObject hawk;
        private JsonObject ntlm;
        private JsonObject oauth1;
        private JsonObject oauth2;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public JsonObject getNoauth() {
            return noauth;
        }

        public void setNoauth(final JsonObject noauth) {
            this.noauth = noauth;
        }

        public JsonObject getApikey() {
            return apikey;
        }

        public void setApikey(final JsonObject apikey) {
            this.apikey = apikey;
        }

        public JsonObject getAwsv4() {
            return awsv4;
        }

        public void setAwsv4(final JsonObject awsv4) {
            this.awsv4 = awsv4;
        }

        public Basic getBasic() {
            return basic;
        }

        public void setBasic(final Basic basic) {
            this.basic = basic;
        }

        public Bearer getBearer() {
            return bearer;
        }

        public void setBearer(final Bearer bearer) {
            this.bearer = bearer;
        }

        public JsonObject getDigest() {
            return digest;
        }

        public void setDigest(final JsonObject digest) {
            this.digest = digest;
        }

        public JsonObject getEdgegrid() {
            return edgegrid;
        }

        public void setEdgegrid(final JsonObject edgegrid) {
            this.edgegrid = edgegrid;
        }

        public JsonObject getHawk() {
            return hawk;
        }

        public void setHawk(final JsonObject hawk) {
            this.hawk = hawk;
        }

        public JsonObject getNtlm() {
            return ntlm;
        }

        public void setNtlm(final JsonObject ntlm) {
            this.ntlm = ntlm;
        }

        public JsonObject getOauth1() {
            return oauth1;
        }

        public void setOauth1(final JsonObject oauth1) {
            this.oauth1 = oauth1;
        }

        public JsonObject getOauth2() {
            return oauth2;
        }

        public void setOauth2(final JsonObject oauth2) {
            this.oauth2 = oauth2;
        }
    }

    public static class Proxy {
        private String match = "http+https://*/*";
        private String host;
        private int port = 8080;
        private Boolean tunnel;
        private Boolean disabled;

        public String getMatch() {
            return match;
        }

        public void setMatch(final String match) {
            this.match = match;
        }

        public String getHost() {
            return host;
        }

        public void setHost(final String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(final int port) {
            this.port = port;
        }

        public Boolean isTunnel() {
            return tunnel;
        }

        public void setTunnel(final Boolean tunnel) {
            this.tunnel = tunnel;
        }

        public Boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }
    }

    public static class Header {
        private String type = "text";
        private String key; // required
        private String value; // required
        private String description;
        private Boolean disabled;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public Boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }
    }

    public static class Request {
        private String url;
        private String method;
        private String description;
        private Body body;
        private List<Header> header;

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(final String method) {
            this.method = method;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public Body getBody() {
            return body;
        }

        public void setBody(final Body body) {
            this.body = body;
        }

        public List<Header> getHeader() {
            return header;
        }

        public void setHeader(final List<Header> header) {
            this.header = header;
        }
    }

    public static class Script {
        private String id;
        private String type;
        private String exec;
        private String src;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getExec() {
            return exec;
        }

        public void setExec(final String exec) {
            this.exec = exec;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(final String src) {
            this.src = src;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Event {
        private String id;
        private String listen; // required
        private Script script;
        private Boolean disabled;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getListen() {
            return listen;
        }

        public void setListen(final String listen) {
            this.listen = listen;
        }

        public Script getScript() {
            return script;
        }

        public void setScript(final Script script) {
            this.script = script;
        }

        public Boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }
    }

    public static class Response {
        private String id;
        private Request originalRequest;
        private List<Header> header;
        private String body;
        private String status;
        private int code;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public Request getOriginalRequest() {
            return originalRequest;
        }

        public void setOriginalRequest(final Request originalRequest) {
            this.originalRequest = originalRequest;
        }

        public List<Header> getHeader() {
            return header;
        }

        public void setHeader(final List<Header> header) {
            this.header = header;
        }

        public String getBody() {
            return body;
        }

        public void setBody(final String body) {
            this.body = body;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }

        public int getCode() {
            return code;
        }

        public void setCode(final int code) {
            this.code = code;
        }
    }

    public static class Item {
        private String id;
        private String name;
        private String description;
        private List<Variable> variable;
        private Event event;
        private Request request; // required
        private List<Response> response;
        private JsonObject protocolProfileBehavior;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public List<Variable> getVariable() {
            return variable;
        }

        public void setVariable(final List<Variable> variable) {
            this.variable = variable;
        }

        public Event getEvent() {
            return event;
        }

        public void setEvent(final Event event) {
            this.event = event;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(final Request request) {
            this.request = request;
        }

        public List<Response> getResponse() {
            return response;
        }

        public void setResponse(final List<Response> response) {
            this.response = response;
        }

        public JsonObject getProtocolProfileBehavior() {
            return protocolProfileBehavior;
        }

        public void setProtocolProfileBehavior(final JsonObject protocolProfileBehavior) {
            this.protocolProfileBehavior = protocolProfileBehavior;
        }
    }

    public static class Info {
        private String name; // required
        private String version;
        private String description;
        private String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(final String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(final String schema) {
            this.schema = schema;
        }
    }
}
