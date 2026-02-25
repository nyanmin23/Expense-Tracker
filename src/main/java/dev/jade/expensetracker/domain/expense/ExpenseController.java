package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    private PagedModel<Expense> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(name = "sort", defaultValue = "entryDate") String field,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sortOrder = direction
                .equalsIgnoreCase("DESC") ?
                Sort.by(field).descending() :
                Sort.by(field).ascending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Expense> expenses = expenseService.findAll(pageable);

        return new PagedModel<>(expenses);
    }

    @PostMapping
    private ResponseEntity<String> createExpense(@RequestBody ExpenseRequest expenseRequest) {

        return ResponseEntity.ok("Hello");
    }

}

