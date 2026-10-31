package com.ayoub.ecommerce.orderline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderLineMapperTests {

    @Test
    void mapsTheParentOrderIdentifier() {
        var mapper = new OrderLineMapper();
        var orderLine = mapper.toOrderLine(new OrderLineRequest(null, 42, 7, 2.0));

        assertEquals(42, orderLine.getOrder().getId());
        assertEquals(7, orderLine.getProductId());
    }
}
