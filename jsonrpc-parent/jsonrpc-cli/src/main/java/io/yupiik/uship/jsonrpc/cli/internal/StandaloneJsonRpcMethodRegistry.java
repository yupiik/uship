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
package io.yupiik.uship.jsonrpc.cli.internal;

import io.yupiik.uship.jsonrpc.core.impl.JsonRpcMethodRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Specializes;

@Specializes
@ApplicationScoped
public class StandaloneJsonRpcMethodRegistry extends JsonRpcMethodRegistry {
    @PostConstruct
    private void initBaseUrl() {
        setBaseUrl("standalone");
    }

    @Override
    protected boolean addClassAsTitleInSchema() {
        return true;
    }
}
