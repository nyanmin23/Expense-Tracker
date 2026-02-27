package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.domain.expense.dto.ExpensePatchRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public PagedModel<ExpenseResponse> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(name = "sort", defaultValue = "entryDate") String field,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sortOrder = direction
                .equalsIgnoreCase("DESC") ?
                Sort.by(field).descending() :
                Sort.by(field).ascending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return new PagedModel<>(expenseService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> addNewExpense(
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(request));
    }

    @PatchMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> editExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpensePatchRequest patch) {
        return ResponseEntity.ok(expenseService.update(expenseId, patch));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long expenseId) {
        expenseService.delete(expenseId);
        return ResponseEntity.noContent().build();
    }
}

