package com.budget.backend.service;


import com.budget.backend.dto.IncomeRequest;
import com.budget.backend.entity.Income;
import com.budget.backend.entity.User;
import com.budget.backend.exception.ResourceNotFoundException;
import com.budget.backend.repository.IncomeRepository;
import com.budget.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class IncomeService {
    
    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Income createIncome(String email, IncomeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setCategory(request.getCategory());
        income.setSource(request.getSource());
        income.setDate(request.getDate());
        income.setNote(request.getNote());
        income.setUser(user);
        
        return incomeRepository.save(income);
    }
    
    public List<Income> getAllIncomes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return incomeRepository.findByUserIdOrderByDateDesc(user.getId());
    }
    
    public List<Income> getFilteredIncomes(String email, LocalDate startDate, LocalDate endDate, String category) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (startDate != null && endDate != null && category != null && !category.isEmpty()) {
            return incomeRepository.findByUserIdAndDateBetweenAndCategoryOrderByDateDesc(
                    user.getId(), startDate, endDate, category);
        } else if (startDate != null && endDate != null) {
            return incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    user.getId(), startDate, endDate);
        } else if (category != null && !category.isEmpty()) {
            return incomeRepository.findByUserIdAndCategoryOrderByDateDesc(user.getId(), category);
        } else {
            return incomeRepository.findByUserIdOrderByDateDesc(user.getId());
        }
    }
    
    public Income getIncomeById(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));
        
        if (!income.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Income not found");
        }
        
        return income;
    }
    
    public Income updateIncome(String email, Long id, IncomeRequest request) {
        Income income = getIncomeById(email, id);
        
        income.setAmount(request.getAmount());
        income.setCategory(request.getCategory());
        income.setSource(request.getSource());
        income.setDate(request.getDate());
        income.setNote(request.getNote());
        
        return incomeRepository.save(income);
    }
    
    public void deleteIncome(String email, Long id) {
        Income income = getIncomeById(email, id);
        incomeRepository.delete(income);
    }
    
    public BigDecimal getTotalIncome(Long userId) {
        BigDecimal total = incomeRepository.getTotalIncomeByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
