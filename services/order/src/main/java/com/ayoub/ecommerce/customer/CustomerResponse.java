package com.ayoub.ecommerce.customer;

public record CustomerResponse(
        Integer id,
        String firstName,
        String lastName,
        String email
) {
}
