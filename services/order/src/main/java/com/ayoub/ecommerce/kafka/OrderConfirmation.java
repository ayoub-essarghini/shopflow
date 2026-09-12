package com.ayoub.ecommerce.kafka;

import com.ayoub.ecommerce.customer.CustomerResponse;
import com.ayoub.ecommerce.order.PaymentMethod;
import com.ayoub.ecommerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
