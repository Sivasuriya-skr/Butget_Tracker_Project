package com.budget.backend.dto;

import lombok.Data;

@Data
public class AIInsightRequest {
    private String query;
    private Integer month;
    private Integer year;
}