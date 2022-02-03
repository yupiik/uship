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
package io.yupiik.uship.jsonrpc.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.yupiik.uship.jsonrpc.quarkus.cdi.JsonRpcBeans;
import org.jboss.jandex.DotName;

import java.util.Map;
import java.util.Set;

public class QuarkusExtension {
    @BuildStep
    AdditionalBeanBuildItem registerBeanClasses() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(DotName.createSimple(JsonRpcBeans.class.getName()).toString())
                .build();
    }


    @BuildStep
    QualifierRegistrarBuildItem enableJsonRpcQualifier() {
        return new QualifierRegistrarBuildItem(() -> Map.of(
                DotName.createSimple("io.yupiik.uship.jsonrpc.core.api.JsonRpc"),
                Set.of()));
    }
}
