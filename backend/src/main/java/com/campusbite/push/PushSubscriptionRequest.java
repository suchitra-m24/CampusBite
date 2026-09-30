package com.campusbite.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequest(
        @NotBlank @Size(max = 500) String endpoint,
        @NotBlank @Size(max = 200) String p256dh,
        @NotBlank @Size(max = 200) String auth
) {
}