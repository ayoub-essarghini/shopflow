package com.ayoub.ecommerce.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotNull(message = "Product id must be present")
        Integer productId,
        @Positive(message = "Quantity is mandatory")
        double quantity
) {
}
