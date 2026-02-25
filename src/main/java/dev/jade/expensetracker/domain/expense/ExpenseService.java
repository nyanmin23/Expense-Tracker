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
    public ExpenseResponse createExpense(ExpenseRequest expenseRequest) {

        User user = userRepository.findById(expenseRequest.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense createdExpense = mapper.toEntity(expenseRequest);
        createdExpense.setUser(user);
        return mapper.toResponse(expenseRepository.save(createdExpense));
    }
}
