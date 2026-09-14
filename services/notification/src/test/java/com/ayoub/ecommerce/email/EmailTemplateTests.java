package com.ayoub.ecommerce.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailTemplateTests {

    @Test
    void everyConfiguredTemplateExists() {
        var classLoader = getClass().getClassLoader();

        for (var emailTemplate : EmailTemplate.values()) {
            assertNotNull(
                    classLoader.getResource("templates/" + emailTemplate.getTemplateName()),
                    emailTemplate.getTemplateName()
            );
        }
    }
}
