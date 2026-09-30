package com.campusbite.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemUpdateRequest(
        @NotNull Long orderItemId,
        @Min(0) @jakarta.validation.constraints.Max(50) int quantity
) {
}
