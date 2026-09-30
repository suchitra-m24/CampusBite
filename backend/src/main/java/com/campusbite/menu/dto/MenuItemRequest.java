package com.campusbite.menu.dto;

import com.campusbite.menu.DietType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull Long categoryId,
        @Size(max = 500) String imageUrl,
        boolean available,
        boolean dailySpecial,
        @Min(1) @Max(120) int baselinePrepTimeMinutes,
        @Size(max = 10) List<@Size(max = 60) String> customizationOptions,
        DietType dietType,
        @Size(max = 20) List<@Size(max = 60) String> allergens
) {
}
