package com.campusbite.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanTokenRequest(@NotBlank String pickupToken) {
}
