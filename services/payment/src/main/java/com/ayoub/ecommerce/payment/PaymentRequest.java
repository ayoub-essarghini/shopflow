package com.ayoub.ecommerce.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        Integer id,
        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be positive")
        BigDecimal amount,
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,
        @NotNull(message = "Order id is required")
        @Positive(message = "Order id must be positive")
        Integer orderId,
        @NotBlank(message = "Order reference is required")
        String orderReference,
        @NotNull(message = "Customer is required")
        @Valid
        Customer customer
) {
}
