package com.campusbite.cart.dto;

import com.campusbite.cart.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartItemDto(
        Long id,
        Long menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        int quantity,
        List<String> selectedCustomizations,
        boolean available
) {
    public static CartItemDto from(CartItem ci) {
        return new CartItemDto(
                ci.getId(),
                ci.getMenuItem().getId(),
                ci.getMenuItem().getName(),
                ci.getMenuItem().getPrice(),
                ci.getQuantity(),
                ci.getSelectedCustomizations(),
                ci.getMenuItem().isAvailable()
        );
    }
}
