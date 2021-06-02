package io.yupiik.uship.samples;

import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.samples.model.Customer;
import jakarta.inject.Inject;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Cdi
public class TestCustomer {

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
