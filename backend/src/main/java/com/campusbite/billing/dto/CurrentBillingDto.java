package com.campusbite.billing.dto;

import java.math.BigDecimal;

public record CurrentBillingDto(int year, int month, int orderCount, BigDecimal runningTotal) {
}