package com.campusbite.order.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOrderItemsRequest(
        @NotEmpty List<OrderItemUpdateRequest> items
) {
}
