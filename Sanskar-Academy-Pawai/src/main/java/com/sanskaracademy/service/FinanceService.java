package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public interface FinanceService {
	
	public BigDecimal getTotalIncome();
	public BigDecimal getTotalExpenses();
	public BigDecimal getNetProfit();
	public  Map<String, BigDecimal> getFinanceSummary();

}
