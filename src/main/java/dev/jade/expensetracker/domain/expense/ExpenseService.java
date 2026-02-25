package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public Page<Expense> findAll(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

//    @Transactional
//    public ExpenseResponse createExpense(ExpenseRequest expenseRequest) {
//
//    }

}
