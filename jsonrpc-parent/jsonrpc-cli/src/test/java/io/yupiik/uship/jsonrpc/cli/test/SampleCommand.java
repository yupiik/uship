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
package io.yupiik.uship.jsonrpc.cli.test;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@JsonRpc
@ApplicationScoped
public class SampleCommand {
    @JsonRpcMethod(name = "sample-to-string")
    public String toString(@JsonRpcParam("first") final String first,
                           @JsonRpcParam("second") final Long second,
                           @JsonRpcParam("third") final Double third,
                           @JsonRpcParam("fourth") final List<String> fourth,
                           @JsonRpcParam("fifth") final Foo fifth,
                           @JsonRpcParam("sixth") final List<Foo> sixth) {
        return first + "," + second + "," + third + "," + fourth + "," + fifth + "," + sixth;
    }

    public static class Foo {
        public String name;

        @Override
        public String toString() {
            return name;
        }
    }
}
