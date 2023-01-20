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
package io.yupiik.uship.jsonrpc.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jsonrpc")
public class JsonRpcConfiguration {
    /**
     * Base URL for OpenRPC method.
     */
    private String baseUrl = "auto";

    /**
     * Servlet mapping for JSON-RPC endpoint.
     */
    private String binding = "/jsonrpc";

    public String getBinding() {
        return binding;
    }

    public JsonRpcConfiguration setBinding(final String binding) {
        this.binding = binding;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public JsonRpcConfiguration setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }
}
