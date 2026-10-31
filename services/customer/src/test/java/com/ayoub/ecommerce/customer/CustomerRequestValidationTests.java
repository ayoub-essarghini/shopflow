package com.ayoub.ecommerce.customer;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomerRequestValidationTests {

    @Test
    void rejectsBlankCustomerDetails() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(
                    new CustomerRequest(null, "", "", "", null)
            );

            assertFalse(violations.isEmpty());
        }
    }
}
