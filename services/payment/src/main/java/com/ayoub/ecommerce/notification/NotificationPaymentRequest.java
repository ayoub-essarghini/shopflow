package com.ayoub.ecommerce.notification;

import com.ayoub.ecommerce.payment.PaymentMethod;

import java.math.BigDecimal;

public record NotificationPaymentRequest(
        String orderReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String customerFirstName,
        String customerLastName,
        String customerEmail
) {
}
