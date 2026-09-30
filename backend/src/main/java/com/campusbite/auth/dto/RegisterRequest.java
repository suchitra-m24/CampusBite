package com.campusbite.auth.dto;

import com.campusbite.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") @Size(max = 120) String fullName,
        @NotBlank @Email(message = "A valid email is required") @Size(max = 180) String email,
        @NotBlank(message = "Login ID is required") @Size(max = 60) String loginId,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotNull(message = "Role is required") Role role,
        @Size(max = 100) String department,
        @Size(max = 30) String academicYear,
        @Size(max = 30) String section
) {
}
