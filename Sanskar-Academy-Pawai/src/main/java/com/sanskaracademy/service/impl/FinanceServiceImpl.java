package com.sanskaracademy.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.sanskaracademy.repository.ExpenseRepository;
import com.sanskaracademy.repository.StudentFeeRepository;
import com.sanskaracademy.service.FinanceService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService{

	private final StudentFeeRepository studentFeeRepository;
	private final ExpenseRepository expenseRepository;

	public BigDecimal getTotalIncome() {
		return studentFeeRepository.getTotalIncome();
	}

	public BigDecimal getTotalExpenses() {
		return expenseRepository.getTotalExpenses();
	}

	public BigDecimal getNetProfit() {
		return getTotalIncome().subtract(getTotalExpenses());
	}

	@Override
	public Map<String, BigDecimal> getFinanceSummary() {

		BigDecimal income = studentFeeRepository.getTotalIncome();
		BigDecimal expenses = expenseRepository.getTotalExpenses();

		if (income == null) income = BigDecimal.ZERO;
		if (expenses == null) expenses = BigDecimal.ZERO;

		BigDecimal profit = income.subtract(expenses);

		Map<String, BigDecimal> map = new HashMap<>();
		map.put("income", income);
		map.put("expenses", expenses);
		map.put("profit", profit);

		return map;
	}
}