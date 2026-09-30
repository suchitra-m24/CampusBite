package com.campusbite.analytics.dto;

public record PeakHourDto(int hourOfDay, double averageLoadPercentage, int sampleCount) {
}
