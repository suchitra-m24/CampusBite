package com.campusbite.billing.dto;

import com.campusbite.billing.MonthlyStatement;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyStatementDto(
        Long id, Long userId, String userName, String loginId, int year, int month,
        int orderCount, BigDecimal totalAmount, boolean settled, Instant settledAt
) {
    public static MonthlyStatementDto from(MonthlyStatement s) {
        return new MonthlyStatementDto(s.getId(), s.getUser().getId(), s.getUser().getFullName(),
                s.getUser().getLoginId(), s.getYear(), s.getMonth(), s.getOrderCount(),
                s.getTotalAmount(), s.isSettled(), s.getSettledAt());
    }
}