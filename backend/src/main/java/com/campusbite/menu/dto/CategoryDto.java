package com.campusbite.menu.dto;

import com.campusbite.menu.Category;

public record CategoryDto(
        Long id,
        String name,
        int displayOrder,
        Integer availableFromMinuteOfDay,
        Integer availableToMinuteOfDay
) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDisplayOrder(),
                c.getAvailableFromMinuteOfDay(), c.getAvailableToMinuteOfDay());
    }
}