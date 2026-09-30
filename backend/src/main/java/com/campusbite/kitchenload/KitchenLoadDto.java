package com.campusbite.kitchenload;

import java.time.Instant;

public record KitchenLoadDto(
        int activeOrderCount,
        int backlogMinutes,
        int loadPercentage,
        String level,
        String message,
        Instant asOf
) {
}
