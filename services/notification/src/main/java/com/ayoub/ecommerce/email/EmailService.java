package com.ayoub.ecommerce.email;

import com.ayoub.ecommerce.kafka.order.Product;
import com.ayoub.ecommerce.kafka.payment.PaymentConfirmation;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.UTF8;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;


    @Async
    public void sendPaymentConfirmationEmail(
            String destinationMail,
            String customerName,
            BigDecimal amount,
            String orderReference
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper msgHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, UTF_8.name());
        msgHelper.setFrom("a.essarghini@gmail.com");

        final String templateName = EmailTemplate.PAYMENT_CONFIRMATION.getTemplateName();

        Map<String, Object> model = new HashMap<>();
        model.put("customerName", customerName);
        model.put("amount", amount);
        model.put("orderReference", orderReference);
        Context context = new Context();
        context.setVariables(model);
        msgHelper.setSubject(EmailTemplate.PAYMENT_CONFIRMATION.getSubject());

        try
        {
            String html = templateEngine.process(templateName, context);
            msgHelper.setTo(destinationMail);
            msgHelper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Email sent to: " + destinationMail + "with template: " + templateName + " and subject: " + EmailTemplate.PAYMENT_CONFIRMATION.getSubject());
        }
        catch (MessagingException e)
        {
            log.warn("Error sending email to: " + destinationMail + " with template: " + templateName + " and subject: " + EmailTemplate.PAYMENT_CONFIRMATION.getSubject());
        }

    }


    @Async
    public void sendOrderConfirmationEmail(
            String destinationMail,
            String customerName,
            BigDecimal amount,
            String orderReference,
            List<Product> products
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper msgHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, UTF_8.name());
        msgHelper.setFrom("a.essarghini@gmail.com");

        final String templateName = EmailTemplate.ORDER_CONFIRMATION.getTemplateName();

        Map<String, Object> model = new HashMap<>();
        model.put("customerName", customerName);
        model.put("amount", amount);
        model.put("orderReference", orderReference);
        model.put("products", products);
        Context context = new Context();
        context.setVariables(model);
        msgHelper.setSubject(EmailTemplate.ORDER_CONFIRMATION.getSubject());

        try
        {
            String html = templateEngine.process(templateName, context);
            msgHelper.setTo(destinationMail);
            msgHelper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Email sent to: " + destinationMail + "with template: " + templateName + " and subject: " + EmailTemplate.ORDER_CONFIRMATION.getSubject());
        }
        catch (MessagingException e)
        {
            log.warn("Error sending email to: " + destinationMail + " with template: " + templateName + " and subject: " + EmailTemplate.ORDER_CONFIRMATION.getSubject());
        }

    }
}
