package com.campusbite.trust;

public record TrustScoreDto(
        int score,
        int totalOrders,
        int noShowCount,
        int cancelledLateCount,
        boolean restricted
) {
    public static TrustScoreDto from(TrustScore t) {
        return new TrustScoreDto(t.getScore(), t.getTotalOrders(), t.getNoShowCount(),
                t.getCancelledLateCount(), t.isRestricted());
    }
}
