package com.ayoub.ecommerce.order;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerContractTests {

    @Test
    void exposesTheGatewayOrderPath() {
        var requestMapping = OrderController.class.getAnnotation(RequestMapping.class);

        assertEquals("/api/v1/orders", requestMapping.value()[0]);
    }
}
