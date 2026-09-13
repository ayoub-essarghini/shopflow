package com.ayoub.ecommerce.kafka;

import com.ayoub.ecommerce.email.EmailService;
import com.ayoub.ecommerce.kafka.order.OrderConfirmation;
import com.ayoub.ecommerce.kafka.payment.PaymentConfirmation;
import com.ayoub.ecommerce.notification.Notification;
import com.ayoub.ecommerce.notification.NotificationRepository;
import com.ayoub.ecommerce.notification.NotificationType;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationConsumer {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @KafkaListener(topics = "payment-topic")
    public void consumePaymentSuccessNotification(PaymentConfirmation paymentConfirmation) throws MessagingException {
        log.info("Received payment success notification <{}>", paymentConfirmation);


        notificationRepository.save(
                Notification.builder().
                        notificationType(NotificationType.PAYMENT_CONFIRMATION).
                        notificationDate(LocalDateTime.now()).
                        paymentConfirmation(paymentConfirmation).
                        build()
        );


        var cusomterName = paymentConfirmation.customerFirstName()+paymentConfirmation.customerLastName();
        emailService.sendPaymentConfirmationEmail(paymentConfirmation.customerEmail(),cusomterName,paymentConfirmation.amount(),paymentConfirmation.orderReference());

    }

    @KafkaListener(topics = "order-topic")
    public void consumeOrderConfirmationNotification(OrderConfirmation orderConfirmation) throws MessagingException {
        log.info("order received success notification <{}>", orderConfirmation);


        notificationRepository.save(
                Notification.builder().
                        notificationType(NotificationType.ORDER_CONFIRMATION).
                        notificationDate(LocalDateTime.now()).
                        orderConfirmation(orderConfirmation).
                        build()
        );

       var customerName = orderConfirmation.customer().firstName()+orderConfirmation.customer().lastName();
        emailService.sendOrderConfirmationEmail(orderConfirmation.customer().email(),customerName,orderConfirmation.totalAmount(),orderConfirmation.orderReference(),orderConfirmation.products());
    }
}
