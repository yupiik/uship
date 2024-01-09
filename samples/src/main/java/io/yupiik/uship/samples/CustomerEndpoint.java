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
package io.yupiik.uship.samples;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import io.yupiik.uship.samples.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;

@JsonRpc
@ApplicationScoped
public class CustomerEndpoint {

    @JsonRpcMethod(name = "getCustomer", documentation = "Retrieve a customer by unique id")
    public Customer getCustomer(@JsonRpcParam final String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirstname("Luke");
        customer.setLastname("Skywalker");
        customer.setTitle("Jedi Master");
        return customer;
    }
}
