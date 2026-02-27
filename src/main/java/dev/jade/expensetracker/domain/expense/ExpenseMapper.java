package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.config.MapStructConfig;
import dev.jade.expensetracker.domain.expense.dto.ExpensePatchRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ExpenseMapper {

    @Mapping(source = "user.userId", target = "userId")
    ExpenseResponse toResponse(Expense expense);

    Expense toEntity(ExpenseRequest request);

    void updateEntityFromRequest(ExpensePatchRequest patch, @MappingTarget Expense expense);

}

