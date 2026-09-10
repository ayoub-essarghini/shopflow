package com.ayoub.ecommerce.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
        String id,
        @NotNull(message = "Customer first name required")
        String firstName,
        @NotNull(message = "Customer last name required")
        String lastName,
        @NotNull(message = "Customer email required")
        @Email(message = "Customer email must be valid")
        String email,
        Address address) {


}
