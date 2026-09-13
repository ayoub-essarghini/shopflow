package com.ayoub.ecommerce.order;

import com.ayoub.ecommerce.customer.CustomerClient;
import com.ayoub.ecommerce.exception.BusinessException;
import com.ayoub.ecommerce.kafka.OrderConfirmation;
import com.ayoub.ecommerce.kafka.OrderProducer;
import com.ayoub.ecommerce.orderline.OrderLineRequest;
import com.ayoub.ecommerce.orderline.OrderLineService;
import com.ayoub.ecommerce.payment.PaymentClient;
import com.ayoub.ecommerce.payment.PaymentRequest;
import com.ayoub.ecommerce.product.ProductClient;
import com.ayoub.ecommerce.product.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper mapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;


    public Integer createOrder(@Valid OrderRequest orderRequest) {
        // check the customer --> OpenFeign
        var customer = customerClient.getCustomerById(orderRequest.customerId())
                .orElseThrow(() -> new BusinessException("No customer exists with the provided id: " + orderRequest.customerId()));


        // purchase products --> product-ms (RestTemplate)
       var purchasedProducts = this.productClient.purchaseProducts(orderRequest.products());

        // persist order
        var order = this.orderRepository.save(mapper.toOrder(orderRequest));

        // persist order lines
        for (PurchaseRequest purchaseRequest : orderRequest.products())
        {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()

                    )
            );
        }


        // todo start payment process --> payment-ms
        var paymentRequest = new PaymentRequest(
                orderRequest.productId(),
                orderRequest.amount(),
                orderRequest.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.createPayment(paymentRequest);



        // send the order confirmation --> notification-ms(kafka)
        orderProducer.sendOrderConfirmation(new OrderConfirmation(
                orderRequest.reference(),
                orderRequest.amount(),
                orderRequest.paymentMethod(),
                customer,
                purchasedProducts

        ));
        return order.getId();
    }

    public List<OrderResponse> findAll() {

        return orderRepository.findAll().stream().map(mapper::fromOrder).toList();
    }

    public OrderResponse findById(Integer orderId) {

        return orderRepository.findById(orderId).map(mapper::fromOrder).orElseThrow(() -> new BusinessException("No order exists with the provided id: " + orderId));
    }
}
