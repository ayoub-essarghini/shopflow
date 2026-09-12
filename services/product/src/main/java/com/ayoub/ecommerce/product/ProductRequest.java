package com.ayoub.ecommerce.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        Integer id,
        @NotNull(message = "Product name is required")
        String name,
        @NotNull(message = "Product name is required")
        String description,
        @Positive(message = "Available quantity must be positive")
        Double availableQuantity,
        @Positive(message = "Price must be positive")
        BigDecimal price,
        @NotNull(message = "Category is required")
        Integer categoryId
) {
}
