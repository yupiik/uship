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
package io.yupiik.uship.samples;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.samples.model.Customer;
import jakarta.inject.Inject;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Cdi
class TestCustomer {
    @Inject
    @JsonRpc
    private CustomerEndpoint customerEndpoint;

    @Test
    void doTest() {
        String id = UUID.randomUUID().toString();
        Customer customer = customerEndpoint.getCustomer(id);
        Assertions.assertAll("customerEndpoint.getCustomer error",
            () -> Assertions.assertNotNull(customer),
            () -> Assertions.assertEquals(id, customer.getId()));
    }
}
