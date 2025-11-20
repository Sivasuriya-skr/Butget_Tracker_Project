package com.budget.backend.controller;

import com.budget.backend.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private ImportService importService;

    @PostMapping("/transactions")
    public ResponseEntity<?> importTransactions(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestParam("file") MultipartFile file) {
        try {
            ImportService.ImportResult result = importService.importTransactions(userDetails.getUsername(), file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import completed");
            response.put("successCount", result.getSuccessCount());
            response.put("failedCount", result.getFailedCount());
            response.put("errors", result.getErrors());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}