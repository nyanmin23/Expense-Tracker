package dev.jade.expensetracker.domain.auth.dto;

import java.time.Instant;

public record AuthResponse(

        Long userId,

        String email,

        Instant createdAt

) {
}
