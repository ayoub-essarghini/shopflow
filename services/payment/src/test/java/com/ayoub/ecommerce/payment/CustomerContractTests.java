package com.ayoub.ecommerce.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerContractTests {

    @Test
    void acceptsTheCustomerServicesStringIdentifier() {
        var idComponent = Customer.class.getRecordComponents()[0];

        assertEquals("id", idComponent.getName());
        assertEquals(String.class, idComponent.getType());
    }
}
