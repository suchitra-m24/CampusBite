package com.campusbite.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryDto(
        long totalOrdersToday,
        BigDecimal revenueToday,
        double averagePrepAccuracy,
        long noShowsToday,
        long cancelledToday
) {
}
