package com.campusbite.common.ws;

public record OrderBroadcastEvent(Long orderId, String status) {}
