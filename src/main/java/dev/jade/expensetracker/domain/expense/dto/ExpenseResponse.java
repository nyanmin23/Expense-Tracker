package dev.jade.expensetracker.domain.expense.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(

        Long expenseId,

        Long userId,

        String description,

        BigDecimal amount,

        LocalDate entryDate,

        Instant createdAt,

        Instant updatedAt

) {
}
