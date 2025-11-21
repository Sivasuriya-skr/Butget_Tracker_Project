package com.budget.backend.service;

import com.budget.backend.dto.AnalyticsResponse;
import com.budget.backend.dto.BudgetRequest;
import com.budget.backend.entity.Budget;
import com.budget.backend.entity.Expense;
import com.budget.backend.entity.Income;
import com.budget.backend.entity.User;
import com.budget.backend.exception.ResourceNotFoundException;
import com.budget.backend.repository.BudgetRepository;
import com.budget.backend.repository.ExpenseRepository;
import com.budget.backend.repository.IncomeRepository;
import com.budget.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    public AnalyticsResponse getAnalytics(String email, Integer month, Integer year) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If month/year not provided, use current month
        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }

        // Get start and end date for the month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Get incomes and expenses for the month
        List<Income> incomes = incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                user.getId(), startDate, endDate);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                user.getId(), startDate, endDate);

        // Calculate totals
        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal savings = balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO;

        // Get budget for the month
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        BigDecimal budgetAmount = budgetOpt.map(Budget::getAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetRemaining = budgetAmount.subtract(totalExpense);
        
        Double budgetUsedPercentage = 0.0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            budgetUsedPercentage = totalExpense.multiply(BigDecimal.valueOf(100))
                    .divide(budgetAmount, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Group income by category
        Map<String, BigDecimal> incomeByCategory = incomes.stream()
                .collect(Collectors.groupingBy(
                        Income::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add)
                ));

        // Group expense by category
        Map<String, BigDecimal> expenseByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // Get monthly trend for last 6 months
        List<AnalyticsResponse.MonthlyData> monthlyTrend = getMonthlyTrend(user.getId(), month, year);

        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setBalance(balance);
        response.setSavings(savings);
        response.setBudget(budgetAmount);
        response.setBudgetRemaining(budgetRemaining);
        response.setBudgetUsedPercentage(budgetUsedPercentage);
        response.setIncomeByCategory(incomeByCategory);
        response.setExpenseByCategory(expenseByCategory);
        response.setMonthlyTrend(monthlyTrend);

        return response;
    }

    private List<AnalyticsResponse.MonthlyData> getMonthlyTrend(Long userId, Integer currentMonth, Integer currentYear) {
        List<AnalyticsResponse.MonthlyData> trend = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.of(currentYear, currentMonth).minusMonths(i);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            List<Income> incomes = incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    userId, startDate, endDate);
            List<Expense> expenses = expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    userId, startDate, endDate);

            BigDecimal income = incomes.stream()
                    .map(Income::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal balance = income.subtract(expense);

            String monthName = yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) 
                    + " " + yearMonth.getYear();

            trend.add(new AnalyticsResponse.MonthlyData(monthName, income, expense, balance));
        }

        return trend;
    }

    public Budget setBudget(String email, BudgetRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<Budget> existingBudget = budgetRepository.findByUserIdAndMonthAndYear(
                user.getId(), request.getMonth(), request.getYear());

        Budget budget;
        if (existingBudget.isPresent()) {
            budget = existingBudget.get();
            budget.setAmount(request.getAmount());
        } else {
            budget = new Budget();
            budget.setAmount(request.getAmount());
            budget.setMonth(request.getMonth());
            budget.setYear(request.getYear());
            budget.setUser(user);
        }

        return budgetRepository.save(budget);
    }

    public Budget getBudget(String email, Integer month, Integer year) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .orElse(null);
    }
}