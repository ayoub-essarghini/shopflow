package com.ayoub.ecommerce.customer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerResponseContractTests {

    @Test
    void usesTheCustomerServicesStringIdentifier() {
        var idComponent = CustomerResponse.class.getRecordComponents()[0];

        assertEquals("id", idComponent.getName());
        assertEquals(String.class, idComponent.getType());
    }
}
