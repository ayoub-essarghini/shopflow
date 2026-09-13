package com.ayoub.ecommerce.payment;

import com.ayoub.ecommerce.customer.CustomerResponse;
import com.ayoub.ecommerce.order.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        Integer id,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer orderId,

        String orderReference,
        CustomerResponse customer
) {
}
