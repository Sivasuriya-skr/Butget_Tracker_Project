package com.budget.backend.controller;


import com.budget.backend.dto.ExpenseRequest;
import com.budget.backend.entity.Expense;
import com.budget.backend.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    
    @Autowired
    private ExpenseService expenseService;
    
    @PostMapping
    public ResponseEntity<Expense> createExpense(@AuthenticationPrincipal UserDetails userDetails,
                                                   @Valid @RequestBody ExpenseRequest request) {
        Expense expense = expenseService.createExpense(userDetails.getUsername(), request);
        return ResponseEntity.ok(expense);
    }
    
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses(@AuthenticationPrincipal UserDetails userDetails,
       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) String category)
      {
        List<Expense> expenses = expenseService.getFilteredExpenses(userDetails.getUsername(), startDate, endDate, category);
        return ResponseEntity.ok(expenses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id) {
        Expense expense = expenseService.getExpenseById(userDetails.getUsername(), id);
        return ResponseEntity.ok(expense);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ExpenseRequest request) {
        Expense expense = expenseService.updateExpense(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(expense);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id) {
        expenseService.deleteExpense(userDetails.getUsername(), id);
        return ResponseEntity.ok().body("Expense deleted successfully");
    }
}
