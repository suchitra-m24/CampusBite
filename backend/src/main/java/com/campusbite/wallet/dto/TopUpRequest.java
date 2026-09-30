package com.campusbite.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

public record TopUpRequest(
        @NotNull @DecimalMin(value = "1.00") @DecimalMax(value = "100000.00") BigDecimal amount
) {
}
