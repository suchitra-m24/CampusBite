package com.campusbite.rating;

import com.campusbite.common.security.SecurityUser;
import com.campusbite.rating.dto.RatingDto;
import com.campusbite.rating.dto.RatingRequest;
import com.campusbite.rating.dto.RatingSummaryDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/api/student/orders/{orderId}/rating")
    public RatingDto submit(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long orderId,
            @Valid @RequestBody RatingRequest request) {

        return ratingService.submit(
                orderId,
                principal.getUser(),
                request
        );
    }

    @GetMapping("/api/student/orders/{orderId}/rating")
    public RatingDto getForOrder(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long orderId) {

        return ratingService.getForOrder(
                orderId,
                principal.getUser()
        );
    }

    @GetMapping("/api/admin/analytics/rating-summary")
    public RatingSummaryDto summary() {
        return ratingService.getAdminSummary();
    }
}