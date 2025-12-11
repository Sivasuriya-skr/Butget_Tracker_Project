package com.budget.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaRequest {
    private String model;
    private String prompt;
    private Boolean stream = false;
    
    @JsonProperty("system")
    private String systemPrompt;
    
    private Options options;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Options {
        private Double temperature;
        
        @JsonProperty("num_predict")
        private Integer numPredict;
    }
}