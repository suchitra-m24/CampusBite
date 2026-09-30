package com.campusbite.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanteenSettingsRequest(
        @NotBlank String canteenName,
        @NotBlank String openingTime,
        @NotBlank String closingTime,
        @NotNull Boolean orderingEnabled
) {}
