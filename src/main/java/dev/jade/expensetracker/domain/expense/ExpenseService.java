package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import dev.jade.expensetracker.domain.user.User;
import dev.jade.expensetracker.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper mapper;

    @Transactional(readOnly = true)
    public Page<Expense> findAll(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Expense createdExpense = mapper.toEntity(request);
        createdExpense.setUser(user);

        return mapper.toResponse(expenseRepository.save(createdExpense));
    }

    @Transactional
    public ExpenseResponse update(Long expenseId, ExpenseRequest request) {

        Expense updatedExpense = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));

        mapper.updateEntityFromRequest(request, updatedExpense);
        return mapper.toResponse(expenseRepository.save(updatedExpense));
    }

    @Transactional
    public void delete(Long expenseId) {

        if (!expenseRepository.existsById(expenseId)) {
            throw new RuntimeException("Expense with ID " + expenseId + " not found");
        }
        expenseRepository.deleteById(expenseId);
    }
}
