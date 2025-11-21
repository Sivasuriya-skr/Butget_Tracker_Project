package com.budget.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal savings;
    private BigDecimal budget;
    private BigDecimal budgetRemaining;
    private Double budgetUsedPercentage;
    
    private Map<String, BigDecimal> incomeByCategory;
    private Map<String, BigDecimal> expenseByCategory;
    
    private List<MonthlyData> monthlyTrend;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyData {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
}
