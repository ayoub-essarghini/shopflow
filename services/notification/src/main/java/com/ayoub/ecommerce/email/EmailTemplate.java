package com.ayoub.ecommerce.email;

import lombok.Getter;

public enum EmailTemplate {
    PAYMENT_CONFIRMATION("payement-confirmation.html","Payment successfully processed"),
    ORDER_CONFIRMATION("order-confirmation.html", "Order Confirmation")
    ;

    @Getter
    private final String templateName;
    @Getter
    private final String subject;

    EmailTemplate(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }
}

