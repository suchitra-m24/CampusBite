package com.campusbite.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;

public record GenerateStatementsRequest(
        @NotNull @Min(2020) Integer year,
        @NotNull @Min(1) @Max(12) Integer month
) {
}