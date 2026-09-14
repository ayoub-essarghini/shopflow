package com.ayoub.ecommerce.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Customer(
        Integer id ,
        @NotBlank(message = "Customer first name required")
        String firstName,
        @NotBlank(message = "Customer last name required")
        String lastName,
        @NotBlank(message = "Customer email required")
        @Email(message = "Customer email must be valid")
        String email
) {
}
