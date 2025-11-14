package com.budget.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Long id;
    private String type; // "income" or "expense"
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDate date;
    private String note;
}
