package com.campusbite.order.dto;

import com.campusbite.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull OrderStatus status) {
}
