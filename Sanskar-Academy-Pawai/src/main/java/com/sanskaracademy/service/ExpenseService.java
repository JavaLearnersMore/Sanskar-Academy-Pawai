package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.util.List;

import com.sanskaracademy.entity.Expense;
import com.sanskaracademy.entity.User;

public interface ExpenseService {

    Expense saveExpense(Expense expense, User recordedBy);

    List<Expense> getAllExpenses();

    void deleteExpense(Long id);

    BigDecimal getTotalExpenses();

    Expense getExpenseById(Long id);
}