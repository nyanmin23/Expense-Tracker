package dev.jade.expensetracker.domain.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpensePatchRequest(

        @Size(min = 3, max = 255, message = "Description must be between 3 and 255 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount format must be valid (up to 10 digits and 2 decimals)")
        BigDecimal amount,

        @PastOrPresent(message = "Entry date cannot be in the future")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate entryDate

) {
}