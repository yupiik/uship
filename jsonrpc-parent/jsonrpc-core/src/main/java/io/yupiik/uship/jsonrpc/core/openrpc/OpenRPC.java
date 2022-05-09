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
package io.yupiik.uship.jsonrpc.core.openrpc;

import io.yupiik.uship.backbone.johnzon.jsonschema.Schema;

import java.util.Collection;
import java.util.Map;

// https://spec.open-rpc.org/
public class OpenRPC {
    private String openrpc = "1.2.4";
    private Info info;
    private Collection<Server> servers;
    private Collection<RpcMethod> methods;
    private Components components;

    public OpenRPC() {
        // no-op
    }

    public OpenRPC(final String openrpc, final Info info, final Collection<Server> servers, final Collection<RpcMethod> methods, final Components components) {
        this.openrpc = openrpc;
        this.info = info;
        this.servers = servers;
        this.methods = methods;
        this.components = components;
    }

    public String getOpenrpc() {
        return openrpc;
    }

    public void setOpenrpc(final String openrpc) {
        this.openrpc = openrpc;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(final Info info) {
        this.info = info;
    }

    public Collection<Server> getServers() {
        return servers;
    }

    public void setServers(final Collection<Server> servers) {
        this.servers = servers;
    }

    public Collection<RpcMethod> getMethods() {
        return methods;
    }

    public void setMethods(final Collection<RpcMethod> methods) {
        this.methods = methods;
    }

    public Components getComponents() {
        return components;
    }

    public void setComponents(final Components components) {
        this.components = components;
    }

    public static class Components {
        private Map<String, Schema> schemas;
        private Map<String, Link> links;
        private Map<String, ErrorValue> errors;
        private Map<String, Tag> tags;

        public Components() {
            // no-op
        }

        public Components(final Map<String, Schema> schemas, final Map<String, Link> links,
                          final Map<String, ErrorValue> errors, final Map<String, Tag> tags) {
            this.schemas = schemas;
            this.links = links;
            this.errors = errors;
            this.tags = tags;
        }

        public Map<String, Schema> getSchemas() {
            return schemas;
        }

        public void setSchemas(final Map<String, Schema> schemas) {
            this.schemas = schemas;
        }

        public Map<String, Link> getLinks() {
            return links;
        }

        public void setLinks(final Map<String, Link> links) {
            this.links = links;
        }

        public Map<String, ErrorValue> getErrors() {
            return errors;
        }

        public void setErrors(final Map<String, ErrorValue> errors) {
            this.errors = errors;
        }

        public Map<String, Tag> getTags() {
            return tags;
        }

        public void setTags(final Map<String, Tag> tags) {
            this.tags = tags;
        }
    }

    public static class Info {
        private String version;
        private String title;
        private String termsOfService;
        private Contact contact;
        private License license;

        public Info() {
            // no-op
        }

        public Info(final String version, final String title, final String termsOfService, final Contact contact, final License license) {
            this.version = version;
            this.title = title;
            this.termsOfService = termsOfService;
            this.contact = contact;
            this.license = license;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(final String version) {
            this.version = version;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getTermsOfService() {
            return termsOfService;
        }

        public void setTermsOfService(final String termsOfService) {
            this.termsOfService = termsOfService;
        }

        public Contact getContact() {
            return contact;
        }

        public void setContact(final Contact contact) {
            this.contact = contact;
        }

        public License getLicense() {
            return license;
        }

        public void setLicense(final License license) {
            this.license = license;
        }
    }

    public static class Contact {
        private String name;
        private String url;
        private String email;

        public Contact() {
            // no-op
        }

        public Contact(final String name, final String url, final String email) {
            this.name = name;
            this.url = url;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }
    }

    public static class License {
        private String name;
        private String url;

        public License() {
            // no-op
        }

        public License(final String name, final String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }
    }

    public static class Server {
        private String name;
        private String url;
        private String summary;
        private Map<String, String> variables;

        public Server() {
            // no-op
        }

        public Server(final String name, final String url, final String summary, final Map<String, String> variables) {
            this.name = name;
            this.url = url;
            this.summary = summary;
            this.variables = variables;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public Map<String, String> getVariables() {
            return variables;
        }

        public void setVariables(final Map<String, String> variables) {
            this.variables = variables;
        }
    }

    public static class ExternalDoc {
        private String description;
        private String url;

        public ExternalDoc() {
            // no-op
        }

        public ExternalDoc(final String description, final String url) {
            this.description = description;
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }
    }

    public static class Tag {
        private String name;
        private String description;
        private String summary;
        private Collection<ExternalDoc> externalDocs;

        public Tag() {
            // no-op
        }

        public Tag(final String name, final String description, final String summary, final Collection<ExternalDoc> externalDocs) {
            this.name = name;
            this.description = description;
            this.summary = summary;
            this.externalDocs = externalDocs;
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

        public String getSummary() {
            return summary;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public Collection<ExternalDoc> getExternalDocs() {
            return externalDocs;
        }

        public void setExternalDocs(final Collection<ExternalDoc> externalDocs) {
            this.externalDocs = externalDocs;
        }
    }

    public static class ErrorValue {
        private int code;
        private String message;
        private Object data;

        public ErrorValue() {
            // no-op
        }

        public ErrorValue(final int code, final String message, final Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public void setCode(final int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(final Object data) {
            this.data = data;
        }
    }

    public static class Link {
        private String name;
        private String description;
        private String summary;
        private String method;
        private Map<String, Object> params;
        private Collection<Server> servers;

        public Link() {
            // no-op
        }

        public Link(final String name, final String description, final String summary, final String method,
                    final Map<String, Object> params, final Collection<Server> servers) {
            this.name = name;
            this.description = description;
            this.summary = summary;
            this.method = method;
            this.params = params;
            this.servers = servers;
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

        public String getSummary() {
            return summary;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(final String method) {
            this.method = method;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(final Map<String, Object> params) {
            this.params = params;
        }

        public Collection<Server> getServers() {
            return servers;
        }

        public void setServers(final Collection<Server> servers) {
            this.servers = servers;
        }
    }

    public static class RpcMethod {
        private String name;
        private Collection<Tag> tags;
        private String summary;
        private String description;
        private Collection<ExternalDoc> externalDocs;
        private Collection<Value> params;
        private Value result;
        private Boolean deprecated;
        private Collection<Server> servers;
        private Collection<ErrorValue> errors;
        private Collection<Link> links;
        private String paramStructure = "either"; // "by-name" | "by-position" | "either"
        private Object examples;

        public RpcMethod() {
            // no-op
        }

        public RpcMethod(final String name, final Collection<Tag> tags, final String summary, final String description,
                         final Collection<ExternalDoc> externalDocs, final Collection<Value> params, final Value result,
                         final Boolean deprecated, final Collection<Server> servers, final Collection<ErrorValue> errors,
                         final Collection<Link> links, final String paramStructure, final Object examples) {
            this.name = name;
            this.tags = tags;
            this.summary = summary;
            this.description = description;
            this.externalDocs = externalDocs;
            this.params = params;
            this.result = result;
            this.deprecated = deprecated;
            this.servers = servers;
            this.errors = errors;
            this.links = links;
            this.paramStructure = paramStructure;
            this.examples = examples;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Collection<Tag> getTags() {
            return tags;
        }

        public void setTags(final Collection<Tag> tags) {
            this.tags = tags;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public Collection<ExternalDoc> getExternalDocs() {
            return externalDocs;
        }

        public void setExternalDocs(final Collection<ExternalDoc> externalDocs) {
            this.externalDocs = externalDocs;
        }

        public Collection<Value> getParams() {
            return params;
        }

        public void setParams(final Collection<Value> params) {
            this.params = params;
        }

        public Value getResult() {
            return result;
        }

        public void setResult(final Value result) {
            this.result = result;
        }

        public Boolean getDeprecated() {
            return deprecated;
        }

        public void setDeprecated(final Boolean deprecated) {
            this.deprecated = deprecated;
        }

        public Collection<Server> getServers() {
            return servers;
        }

        public void setServers(final Collection<Server> servers) {
            this.servers = servers;
        }

        public Collection<ErrorValue> getErrors() {
            return errors;
        }

        public void setErrors(final Collection<ErrorValue> errors) {
            this.errors = errors;
        }

        public Collection<Link> getLinks() {
            return links;
        }

        public void setLinks(final Collection<Link> links) {
            this.links = links;
        }

        public String getParamStructure() {
            return paramStructure;
        }

        public void setParamStructure(final String paramStructure) {
            this.paramStructure = paramStructure;
        }

        public Object getExamples() {
            return examples;
        }

        public void setExamples(final Object examples) {
            this.examples = examples;
        }
    }

    public static class Value {
        private String name;
        private String description;
        private Schema schema;
        private Boolean required;
        private Boolean deprecated;

        public Value() {
            // no-op
        }

        public Value(final String name, final String description, final Schema schema, final Boolean required, final Boolean deprecated) {
            this.name = name;
            this.description = description;
            this.schema = schema;
            this.required = required;
            this.deprecated = deprecated;
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

        public Schema getSchema() {
            return schema;
        }

        public void setSchema(final Schema schema) {
            this.schema = schema;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(final Boolean required) {
            this.required = required;
        }

        public Boolean getDeprecated() {
            return deprecated;
        }

        public void setDeprecated(final Boolean deprecated) {
            this.deprecated = deprecated;
        }
    }
}
