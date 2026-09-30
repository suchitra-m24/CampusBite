package com.campusbite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 60) String loginId,
        @NotBlank String password
) {
}
