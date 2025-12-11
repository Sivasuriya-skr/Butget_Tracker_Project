package com.budget.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OllamaResponse {
    private String model;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    private String response;
    private Boolean done;
    
    @JsonProperty("total_duration")
    private Long totalDuration;
}