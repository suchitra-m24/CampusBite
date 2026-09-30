package com.campusbite.auth.dto;

/** currentRefreshToken is optional -- when provided, lets the session list mark which entry is "this device". */
public record SessionsRequest(String currentRefreshToken) {
}
