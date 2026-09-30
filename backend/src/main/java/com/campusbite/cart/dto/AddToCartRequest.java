package com.campusbite.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddToCartRequest(
        @NotNull Long menuItemId,
        @Min(1) @Max(50) int quantity,
        List<String> selectedCustomizations
) {
}
