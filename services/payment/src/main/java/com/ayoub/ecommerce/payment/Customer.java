package com.ayoub.ecommerce.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record Customer(
        Integer id ,
        @NotNull(message = "Customer first name required")
        String firstName,
        @NotNull(message = "Customer last name required")
        String lastName,
        @NotNull(message = "Customer email required")
        @Email(message = "Customer email must be valid")
        String email
) {
}
