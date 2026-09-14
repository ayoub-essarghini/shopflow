package com.ayoub.ecommerce.payment;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PaymentContractTests {

    @Test
    void exposesThePluralPaymentsPath() {
        var requestMapping = PaymentController.class.getAnnotation(RequestMapping.class);

        assertEquals("/api/v1/payments", requestMapping.value()[0]);
    }

    @Test
    void rejectsAnEmptyPaymentRequest() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(
                    new PaymentRequest(null, null, null, null, null, null)
            );

            assertFalse(violations.isEmpty());
        }
    }
}
