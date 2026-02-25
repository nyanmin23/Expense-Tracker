package dev.jade.expensetracker.domain.expense;

import dev.jade.expensetracker.config.MapStructConfig;
import dev.jade.expensetracker.domain.expense.dto.ExpenseRequest;
import dev.jade.expensetracker.domain.expense.dto.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ExpenseMapper {

    @Mapping(source = "user.userId", target = "userId")
    ExpenseResponse toResponse(Expense expense);

    @Mapping(source = "userId", target = "user.userId")
    Expense toEntity(ExpenseRequest request);

}

