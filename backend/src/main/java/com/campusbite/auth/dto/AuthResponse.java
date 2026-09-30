package com.campusbite.auth.dto;

import com.campusbite.user.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user,
        /** Dev-mode stand-in for an emailed verification link -- only populated right after registration. */
        String devVerificationLink
) {
}
