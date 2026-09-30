package com.campusbite.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnsubscribeRequest(@NotBlank @Size(max = 500) String endpoint) {
}