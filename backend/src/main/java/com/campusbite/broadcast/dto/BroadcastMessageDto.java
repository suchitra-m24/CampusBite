package com.campusbite.broadcast.dto;

import com.campusbite.broadcast.BroadcastMessage;

public record BroadcastMessageDto(Long id, String message, boolean active) {
    public static BroadcastMessageDto from(BroadcastMessage b) {
        return new BroadcastMessageDto(b.getId(), b.getMessage(), b.isActive());
    }
}
