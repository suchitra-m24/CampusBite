package com.campusbite.auth.dto;

import jakarta.validation.constraints.NotNull;

public record RevokeSessionRequest(
        @NotNull Long sessionId
) {
}
