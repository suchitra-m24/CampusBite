package com.campusbite.auth.dto;

public record ForgotPasswordResponse(
        String message,
        /** Only populated when explicit local development reset links are enabled. */
        String devResetLink
) {
}
