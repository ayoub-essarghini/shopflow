package com.ayoub.ecommerce.kafka.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderConfirmationContractTests {

    @Test
    void usesTheSameAmountFieldAsTheOrderProducer() {
        var amountComponent = OrderConfirmation.class.getRecordComponents()[1];

        assertEquals("amount", amountComponent.getName());
        assertEquals(BigDecimal.class, amountComponent.getType());
    }
}
