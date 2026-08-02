package com.tangent.dto;

public record AuthResponse(String token, String type, UserResponse user) {
}
