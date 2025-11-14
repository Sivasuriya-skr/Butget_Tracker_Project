package com.budget.backend.service;


import com.budget.backend.dto.ExpenseRequest;
import com.budget.backend.entity.Expense;
import com.budget.backend.entity.User;
import com.budget.backend.exception.ResourceNotFoundException;
import com.budget.backend.repository.ExpenseRepository;
import com.budget.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseService {
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Expense createExpense(String email, ExpenseRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setDate(request.getDate());
        expense.setNote(request.getNote());
        expense.setUser(user);
        
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getAllExpenses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return expenseRepository.findByUserIdOrderByDateDesc(user.getId());
    }
    
    public List<Expense> getFilteredExpenses(String email, LocalDate startDate, LocalDate endDate, String category) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (startDate != null && endDate != null && category != null && !category.isEmpty()) {
            return expenseRepository.findByUserIdAndDateBetweenAndCategoryOrderByDateDesc(
                    user.getId(), startDate, endDate, category);
        } else if (startDate != null && endDate != null) {
            return expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    user.getId(), startDate, endDate);
        } else if (category != null && !category.isEmpty()) {
            return expenseRepository.findByUserIdAndCategoryOrderByDateDesc(user.getId(), category);
        } else {
            return expenseRepository.findByUserIdOrderByDateDesc(user.getId());
        }
    }
    
    public Expense getExpenseById(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Expense not found");
        }
        
        return expense;
    }
    
    public Expense updateExpense(String email, Long id, ExpenseRequest request) {
        Expense expense = getExpenseById(email, id);
        
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setDate(request.getDate());
        expense.setNote(request.getNote());
        
        return expenseRepository.save(expense);
    }
    
    public void deleteExpense(String email, Long id) {
        Expense expense = getExpenseById(email, id);
        expenseRepository.delete(expense);
    }
    
    public BigDecimal getTotalExpense(Long userId) {
        BigDecimal total = expenseRepository.getTotalExpenseByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
