package com.mystreet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        @JsonProperty("isAdmin") boolean isAdmin) {
}
