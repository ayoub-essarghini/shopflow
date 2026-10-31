package com.ayoub.ecommerce.product;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductRequestValidationTests {

    @Test
    void rejectsMissingProductValues() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(
                    new ProductRequest(null, "Test product", "Test product", null, null, 1)
            );

            assertFalse(violations.isEmpty());
        }
    }

    @Test
    void rejectsNegativePurchaseQuantities() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(new PurchaseRequest(1, -1.0));

            assertFalse(violations.isEmpty());
        }
    }
}
