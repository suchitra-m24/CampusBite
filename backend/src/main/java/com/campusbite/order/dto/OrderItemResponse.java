package com.campusbite.order.dto;

import com.campusbite.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        Long orderItemId,
        Long menuItemId,
        String menuItemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        List<String> selectedCustomizations
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getMenuItemNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getSelectedCustomizations()
        );
    }
}
