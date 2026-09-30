package com.campusbite.auth.dto;

import com.campusbite.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 60) String loginId,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role
) {
}