package com.campusbite.analytics;

import com.campusbite.analytics.dto.AnalyticsSummaryDto;
import com.campusbite.analytics.dto.BestSellerDto;
import com.campusbite.analytics.dto.PeakHourDto;
import com.campusbite.analytics.dto.SlotUtilizationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryDto summary() {
        return analyticsService.summary();
    }

    @GetMapping("/best-sellers")
    public List<BestSellerDto> bestSellers(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.bestSellers(limit);
    }

    @GetMapping("/peak-hours")
    public List<PeakHourDto> peakHours() {
        return analyticsService.peakHours();
    }

    @GetMapping("/slot-utilization")
    public List<SlotUtilizationDto> slotUtilization() {
        return analyticsService.slotUtilization();
    }
}
