package com.budget.backend.service;

import com.budget.backend.entity.Expense;
import com.budget.backend.entity.Income;
import com.budget.backend.entity.User;
import com.budget.backend.exception.BadRequestException;
import com.budget.backend.repository.ExpenseRepository;
import com.budget.backend.repository.IncomeRepository;
import com.budget.backend.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy")
    };

    public ImportResult importTransactions(String email, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("Invalid file");
        }

        ImportResult result = new ImportResult();

        try {
            if (filename.endsWith(".csv")) {
                result = importFromCSV(file, user);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                result = importFromExcel(file, user);
            } else {
                throw new BadRequestException("Only CSV and Excel files are supported");
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to import file: " + e.getMessage());
        }

        return result;
    }

    private ImportResult importFromCSV(MultipartFile file, User user) throws Exception {
        ImportResult result = new ImportResult();
        List<Income> incomes = new ArrayList<>();
        List<Expense> expenses = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                try {
                    String type = record.get("Type").trim();
                    String dateStr = record.get("Date").trim();
                    String category = record.get("Category").trim();
                    String description = record.get("Description").trim();
                    String amountStr = record.get("Amount").trim();
                    String note = record.isMapped("Note") ? record.get("Note").trim() : "";

                    // Parse amount
                    BigDecimal amount = new BigDecimal(amountStr.replace(",", ""));

                    // Parse date
                    LocalDate date = parseDate(dateStr);

                    if ("Income".equalsIgnoreCase(type)) {
                        Income income = new Income();
                        income.setAmount(amount);
                        income.setCategory(category);
                        income.setSource(description);
                        income.setDate(date);
                        income.setNote(note);
                        income.setUser(user);
                        incomes.add(income);
                    } else if ("Expense".equalsIgnoreCase(type)) {
                        Expense expense = new Expense();
                        expense.setAmount(amount);
                        expense.setCategory(category);
                        expense.setDescription(description);
                        expense.setDate(date);
                        expense.setNote(note);
                        expense.setUser(user);
                        expenses.add(expense);
                    }

                    result.incrementSuccess();
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError("Row " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        }

        // Save all valid records
        if (!incomes.isEmpty()) {
            incomeRepository.saveAll(incomes);
        }
        if (!expenses.isEmpty()) {
            expenseRepository.saveAll(expenses);
        }

        return result;
    }

    private ImportResult importFromExcel(MultipartFile file, User user) throws Exception {
        ImportResult result = new ImportResult();
        List<Income> incomes = new ArrayList<>();
        List<Expense> expenses = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("Excel file is empty");
            }

            // Find column indices
            int dateCol = -1, typeCol = -1, categoryCol = -1, descCol = -1, amountCol = -1, noteCol = -1;

            for (Cell cell : headerRow) {
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "date": dateCol = cell.getColumnIndex(); break;
                    case "type": typeCol = cell.getColumnIndex(); break;
                    case "category": categoryCol = cell.getColumnIndex(); break;
                    case "description": descCol = cell.getColumnIndex(); break;
                    case "amount": amountCol = cell.getColumnIndex(); break;
                    case "note": noteCol = cell.getColumnIndex(); break;
                }
            }

            if (dateCol == -1 || typeCol == -1 || categoryCol == -1 || descCol == -1 || amountCol == -1) {
                throw new BadRequestException("Excel file must have Date, Type, Category, Description, and Amount columns");
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String type = getCellValueAsString(row.getCell(typeCol)).trim();
                    String dateStr = getCellValueAsString(row.getCell(dateCol)).trim();
                    String category = getCellValueAsString(row.getCell(categoryCol)).trim();
                    String description = getCellValueAsString(row.getCell(descCol)).trim();
                    String amountStr = getCellValueAsString(row.getCell(amountCol)).trim();
                    String note = noteCol != -1 ? getCellValueAsString(row.getCell(noteCol)).trim() : "";

                    // Skip empty rows
                    if (type.isEmpty() || dateStr.isEmpty() || amountStr.isEmpty()) {
                        continue;
                    }

                    // Parse amount
                    BigDecimal amount = new BigDecimal(amountStr.replace(",", ""));

                    // Parse date
                    LocalDate date = parseDate(dateStr);

                    if ("Income".equalsIgnoreCase(type)) {
                        Income income = new Income();
                        income.setAmount(amount);
                        income.setCategory(category);
                        income.setSource(description);
                        income.setDate(date);
                        income.setNote(note);
                        income.setUser(user);
                        incomes.add(income);
                    } else if ("Expense".equalsIgnoreCase(type)) {
                        Expense expense = new Expense();
                        expense.setAmount(amount);
                        expense.setCategory(category);
                        expense.setDescription(description);
                        expense.setDate(date);
                        expense.setNote(note);
                        expense.setUser(user);
                        expenses.add(expense);
                    }

                    result.incrementSuccess();
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        // Save all valid records
        if (!incomes.isEmpty()) {
            incomeRepository.saveAll(incomes);
        }
        if (!expenses.isEmpty()) {
            expenseRepository.saveAll(expenses);
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        throw new BadRequestException("Invalid date format: " + dateStr + ". Supported formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy");
    }

    public static class ImportResult {
        private int successCount = 0;
        private int failedCount = 0;
        private List<String> errors = new ArrayList<>();

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailed() {
            failedCount++;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
