package com.ayoub.ecommerce.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductClientConfigurationTests {

    @Test
    void usesTheCompleteProductUrlPlaceholder() throws NoSuchFieldException {
        var productUrlField = ProductClient.class.getDeclaredField("productUrl");
        var value = productUrlField.getAnnotation(Value.class);

        assertEquals("${application.config.product-url}", value.value());
    }
}
