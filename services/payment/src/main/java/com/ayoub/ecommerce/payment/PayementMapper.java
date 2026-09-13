package com.ayoub.ecommerce.payment;

import org.springframework.stereotype.Service;

@Service
public class PayementMapper {
    public Payment toPayment(PaymentRequest request) {
        return Payment
                .builder()
                .id(request.id())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .orderId(request.orderId())
                .build();
    }
}
