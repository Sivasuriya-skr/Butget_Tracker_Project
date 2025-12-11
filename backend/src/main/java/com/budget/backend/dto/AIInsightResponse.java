package com.budget.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIInsightResponse {
    private String insight;
    private List<String> recommendations;
    private String spendingPattern;
    private String savingTip;
    private Double financialHealthScore;
    private String riskAssessment;
}