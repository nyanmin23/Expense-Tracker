package dev.jade.expensetracker.domain.auth.dto;

public record RegisterUserRequest(
        String email,

        String password,

        String verifyPassword
) {
}
