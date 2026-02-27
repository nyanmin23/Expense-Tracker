package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.common.ResourceNotFoundException;
import dev.jade.expensetracker.domain.expense.dto.ExpensePatchRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import dev.jade.expensetracker.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper mapper;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> findAll(Pageable pageable) {
        User currentUser = getCurrentUser();
        return expenseRepository
                .findAllByUser(currentUser, pageable)
                .map(mapper::toResponse);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        User currentUser = getCurrentUser();
        Expense expense = mapper.toEntity(request);
        expense.setUser(currentUser);
        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long expenseId, ExpensePatchRequest patch) {
        User currentUser = getCurrentUser();
        Expense expense = expenseRepository
                .findByExpenseIdAndUser(expenseId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found or access denied"));


        mapper.updateEntityFromRequest(patch, expense);
        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long expenseId) {
        User currentUser = getCurrentUser();
        Expense expense = expenseRepository
                .findByExpenseIdAndUser(expenseId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found or access denied"));

        expenseRepository.delete(expense);
    }
}
