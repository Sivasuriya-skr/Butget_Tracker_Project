package com.budget.backend.controller;

import com.budget.backend.dto.AIInsightRequest;
import com.budget.backend.dto.AIInsightResponse;
import com.budget.backend.service.AIInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIInsightService aiInsightService;

    @GetMapping("/insights")
    public ResponseEntity<AIInsightResponse> getInsights(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        AIInsightResponse insights = aiInsightService.getFinancialInsights(
                userDetails.getUsername(), month, year);
        return ResponseEntity.ok(insights);
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AIInsightRequest request) {
        
        String response = aiInsightService.getChatResponse(
                userDetails.getUsername(), 
                request.getQuery(), 
                request.getMonth(), 
                request.getYear());
        
        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        return ResponseEntity.ok(result);
    }
}
