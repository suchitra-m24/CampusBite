package com.campusbite.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(0) @jakarta.validation.constraints.Max(50) int quantity
) {
}
