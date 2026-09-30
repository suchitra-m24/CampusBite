package com.campusbite.cart.dto;

import com.campusbite.cart.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(List<CartItemDto> items, BigDecimal total) {
    public static CartDto from(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(CartItemDto::from).toList();
        BigDecimal total = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(items, total);
    }
}
