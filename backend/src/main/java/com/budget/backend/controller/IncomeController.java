package com.budget.backend.controller;


import com.budget.backend.dto.IncomeRequest;
import com.budget.backend.entity.Income;
import com.budget.backend.service.IncomeService;
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
@RequestMapping("/api/incomes")
public class IncomeController {
    
    @Autowired
    private IncomeService incomeService;
    
    @PostMapping
    public ResponseEntity<Income> createIncome(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody IncomeRequest request) {
        Income income = incomeService.createIncome(userDetails.getUsername(), request);
        return ResponseEntity.ok(income);
    }
    
    @GetMapping
    public ResponseEntity<List<Income>> getAllIncomes(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                        @RequestParam(required = false) String category) {
        List<Income> incomes = incomeService.getFilteredIncomes(userDetails.getUsername(), startDate, endDate, category);
        return ResponseEntity.ok(incomes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Income> getIncomeById(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long id) {
        Income income = incomeService.getIncomeById(userDetails.getUsername(), id);
        return ResponseEntity.ok(income);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Income> updateIncome(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody IncomeRequest request) {
        Income income = incomeService.updateIncome(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(income);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIncome(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        incomeService.deleteIncome(userDetails.getUsername(), id);
        return ResponseEntity.ok().body("Income deleted successfully");
    }
}
