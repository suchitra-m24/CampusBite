package com.campusbite.order.dto;

import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        @Size(max = 300, message = "Special instructions must be at most 300 characters")
        String specialInstructions) {
}
