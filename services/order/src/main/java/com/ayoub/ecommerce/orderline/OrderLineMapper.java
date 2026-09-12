package com.ayoub.ecommerce.orderline;

import com.ayoub.ecommerce.order.Order;
import org.springframework.stereotype.Service;


@Service
public class OrderLineMapper {
    public OrderLine toOrderLine(OrderLineRequest orderLineRequest) {
        return OrderLine.builder()
                .id(orderLineRequest.id())
                .quantity(orderLineRequest.quantity())
                .productId(orderLineRequest.productId())
                .order(Order.builder().id(orderLineRequest.id()).build())
                .build();
    }

    public OrderLineResponse fromOrderLine(OrderLine orderLine) {

        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getQuantity()
        );
    }
}
