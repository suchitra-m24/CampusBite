package com.campusbite.rating.dto;

import com.campusbite.rating.Rating;

public record RatingDto(Long orderId, int stars, String comment) {
    public static RatingDto from(Rating r) {
        return new RatingDto(r.getOrder().getId(), r.getStars(), r.getComment());
    }
}
