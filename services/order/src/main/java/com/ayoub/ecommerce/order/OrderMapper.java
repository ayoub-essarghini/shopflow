package com.ayoub.ecommerce.order;

import org.springframework.stereotype.Service;

@Service
public class OrderMapper {
    public Order toOrder(OrderRequest orderRequest) {

        return Order.builder().id(orderRequest.productId()).customerId(orderRequest.customerId()).reference(orderRequest.reference()).totalAmount(orderRequest.amount()).paymentMethod(orderRequest.paymentMethod()).build();


    }

    public OrderResponse fromOrder(Order order) {

        return new OrderResponse(
            order.getId(),
                order.getReference(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getCustomerId()
        );
    }
}
