package com.budget.backend.controller;


import com.budget.backend.dto.ChangePasswordRequest;
import com.budget.backend.dto.DashboardResponse;
import com.budget.backend.dto.TransactionResponse;
import com.budget.backend.dto.UpdateProfileRequest;
import com.budget.backend.entity.Expense;
import com.budget.backend.entity.Income;
import com.budget.backend.entity.User;
import com.budget.backend.repository.ExpenseRepository;
import com.budget.backend.repository.IncomeRepository;
import com.budget.backend.service.ExpenseService;
import com.budget.backend.service.IncomeService;
import com.budget.backend.service.UserService;
import jakarta.validation.Valid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
//import java.util.stream.Stream;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private IncomeService incomeService;
    
    @Autowired
    private ExpenseService expenseService;
    
    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/profile/photo")
    public ResponseEntity<User> uploadProfilePhoto(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestParam("file") MultipartFile file) {
        User user = userService.uploadProfilePhoto(userDetails.getUsername(), file);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
    }
    
    @PutMapping("/currency")
    public ResponseEntity<User> updateCurrency(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestBody Map<String, String> request) {
        User user = userService.updateCurrency(userDetails.getUsername(), request.get("currency"));
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        
        BigDecimal totalIncome = incomeService.getTotalIncome(user.getId());
        BigDecimal totalExpense = expenseService.getTotalExpense(user.getId());
        BigDecimal balance = totalIncome.subtract(totalExpense);
        
        // Get recent 5 transactions
        List<Income> recentIncomes = incomeRepository.findTop5ByUserIdOrderByDateDesc(user.getId());
        List<Expense> recentExpenses = expenseRepository.findTop5ByUserIdOrderByDateDesc(user.getId());
        
        List<TransactionResponse> transactions = new ArrayList<>();
        
        // Convert incomes to transaction responses
        for (Income income : recentIncomes) {
            transactions.add(new TransactionResponse(
                    income.getId(),
                    "income",
                    income.getAmount(),
                    income.getCategory(),
                    income.getSource(),
                    income.getDate(),
                    income.getNote()
            ));
        }
        
        // Convert expenses to transaction responses
        for (Expense expense : recentExpenses) {
            transactions.add(new TransactionResponse(
                    expense.getId(),
                    "expense",
                    expense.getAmount(),
                    expense.getCategory(),
                    expense.getDescription(),
                    expense.getDate(),
                    expense.getNote()
            ));
        }
        
        // Sort by date descending and take top 5
        transactions = transactions.stream()
                .sorted(Comparator.comparing(TransactionResponse::getDate).reversed())
                .limit(5)
                .collect(Collectors.toList());
        
        DashboardResponse response = new DashboardResponse(totalIncome, totalExpense, balance, transactions);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/export")
    public ResponseEntity<String> exportTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        
        List<Income> incomes = incomeRepository.findByUserIdOrderByDateDesc(user.getId());
        List<Expense> expenses = expenseRepository.findByUserIdOrderByDateDesc(user.getId());
        
        try {
            StringWriter writer = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader("Date", "Type", "Category", "Description", "Amount", "Note"));
            
            // Write incomes
            for (Income income : incomes) {
                csvPrinter.printRecord(
                        income.getDate(),
                        "Income",
                        income.getCategory(),
                        income.getSource(),
                        income.getAmount(),
                        income.getNote()
                );
            }
            
            // Write expenses
            for (Expense expense : expenses) {
                csvPrinter.printRecord(
                        expense.getDate(),
                        "Expense",
                        expense.getCategory(),
                        expense.getDescription(),
                        expense.getAmount(),
                        expense.getNote()
                );
            }
            
            csvPrinter.flush();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "transactions.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(writer.toString());
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok().body(Map.of("message", "Account deleted successfully"));
    }
}
