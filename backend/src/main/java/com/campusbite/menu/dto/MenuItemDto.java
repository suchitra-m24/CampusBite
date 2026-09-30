package com.campusbite.menu.dto;

import com.campusbite.menu.DietType;
import com.campusbite.menu.MenuItem;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        String imageUrl,
        boolean available,
        boolean dailySpecial,
        int baselinePrepTimeMinutes,
        List<String> customizationOptions,
        DietType dietType,
        List<String> allergens
) {
    public static MenuItemDto from(MenuItem m) {
        return new MenuItemDto(
                m.getId(), m.getName(), m.getDescription(), m.getPrice(),
                m.getCategory() != null ? m.getCategory().getId() : null,
                m.getCategory() != null ? m.getCategory().getName() : null,
                m.getImageUrl(), m.isAvailable(), m.isDailySpecial(),
                m.getBaselinePrepTimeMinutes(), m.getCustomizationOptions(),
                m.getDietType(), m.getAllergens());
    }
}
