package com.campusbite.order.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 300, message = "Cancellation reason must be at most 300 characters")
        String reason
) {}
