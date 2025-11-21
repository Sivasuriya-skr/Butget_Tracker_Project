package com.budget.backend.controller;

import com.budget.backend.dto.AnalyticsResponse;
import com.budget.backend.dto.BudgetRequest;
import com.budget.backend.entity.Budget;
import com.budget.backend.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        AnalyticsResponse analytics = analyticsService.getAnalytics(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/budget")
    public ResponseEntity<Budget> setBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BudgetRequest request) {
        
        Budget budget = analyticsService.setBudget(userDetails.getUsername(), request);
        return ResponseEntity.ok(budget);
    }

    @GetMapping("/budget")
    public ResponseEntity<Budget> getBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        Budget budget = analyticsService.getBudget(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(budget);
    }
}