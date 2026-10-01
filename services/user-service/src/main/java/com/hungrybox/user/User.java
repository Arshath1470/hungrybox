package com.hungrybox.user;

import java.time.Instant;

public record User(
        long id,
        String fullName,
        String email,
        String passwordHash,
        String phone,
        String address,
        String role,
        Instant createdAt) {
}
