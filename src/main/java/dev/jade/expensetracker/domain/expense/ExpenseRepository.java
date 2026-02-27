package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findAllByUser(User user, Pageable pageable);

    Optional<Expense> findByExpenseIdAndUser(Long expenseId, User user);

}
