package com.campusbite.order.dto;

import com.campusbite.order.Order;
import com.campusbite.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long studentId,
        String studentName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String pickupToken,
        String qrCodeBase64,
        Instant pickupSlotStart,
        Instant pickupSlotEnd,
        int estimatedPrepMinutes,
        Integer actualPrepMinutes,
        String specialInstructions,
        boolean delayNotified,
        Instant placedAt,
        Instant confirmedAt,
        Instant preparingAt,
        Instant readyAt,
        Instant pickedUpAt,
        Instant cancelledAt,
        String cancelReason,
        boolean overdue
) {
    public static OrderResponse from(Order o, String qrCodeBase64) {
        return new OrderResponse(
                o.getId(),
                o.getStudent().getId(),
                o.getStudent().getFullName(),
                o.getStatus(),
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                o.getTotalAmount(),
                o.getPickupToken(),
                qrCodeBase64,
                o.getPickupSlotStart(),
                o.getPickupSlotEnd(),
                o.getEstimatedPrepMinutes(),
                o.getActualPrepMinutes(),
                o.getSpecialInstructions(),
                o.isDelayNotified(),
                o.getPlacedAt(),
                o.getConfirmedAt(),
                o.getPreparingAt(),
                o.getReadyAt(),
                o.getPickedUpAt(),
                o.getCancelledAt(),
                o.getCancelReason(),
                o.getStatus() == OrderStatus.PREPARING && o.getPickupSlotEnd().isBefore(Instant.now())
        );
    }
}
