package com.campusbite.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 255) String name,
        @Min(0) int displayOrder,
        @Min(0) @Max(1439) Integer availableFromMinuteOfDay,
        @Min(0) @Max(1439) Integer availableToMinuteOfDay
) {
}
