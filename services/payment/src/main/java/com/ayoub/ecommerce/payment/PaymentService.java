package com.ayoub.ecommerce.payment;

import com.ayoub.ecommerce.notification.NotificationPaymentRequest;
import com.ayoub.ecommerce.notification.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PayementMapper mapper;
    private final NotificationProducer notificationProducer;

    @Transactional
    public Integer createPayment(PaymentRequest request) {
        var payment = paymentRepository.save(mapper.toPayment(request));

    notificationProducer.sendNotification(
            new NotificationPaymentRequest(
                    request.orderReference(),
                    request.amount(),
                    request.paymentMethod(),
                    request.customer().firstName(),
                    request.customer().lastName(),
                    request.customer().email()
            )
    );
        return payment.getId();
    }
}
