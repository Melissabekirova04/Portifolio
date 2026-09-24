package org.example.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {
}