package com.campusbite.broadcast.dto;

import jakarta.validation.constraints.NotBlank;

public record SetBroadcastRequest(
        @NotBlank String message
) {
}
