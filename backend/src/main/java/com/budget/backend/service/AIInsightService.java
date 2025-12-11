package com.budget.backend.service;

import com.budget.backend.dto.*;
import com.budget.backend.entity.User;
import com.budget.backend.exception.BadRequestException;
import com.budget.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class AIInsightService {  // Fixed: Was AlInsightService

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Value("${ai.provider:ollama}")
    private String aiProvider;

    public AIInsightResponse getFinancialInsights(String email, Integer month, Integer year) {  // Fixed: Was AlInsightResponse
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }

        AnalyticsResponse analytics = analyticsService.getAnalytics(email, month, year);

        FinancialDataContext context = new FinancialDataContext();
        context.setTotalIncome(analytics.getTotalIncome());
        context.setTotalExpense(analytics.getTotalExpense());
        context.setBalance(analytics.getBalance());
        context.setSavings(analytics.getSavings());
        context.setBudget(analytics.getBudget());
        context.setBudgetUsedPercentage(analytics.getBudgetUsedPercentage());
        context.setIncomeByCategory(analytics.getIncomeByCategory());
        context.setExpenseByCategory(analytics.getExpenseByCategory());
        context.setMonth(YearMonth.of(year, month).getMonth().name());
        context.setYear(String.valueOf(year));

        if (!ollamaService.isAvailable()) {
            System.out.println("Ollama not available, using fallback insights");
            return generateFallbackInsights(context);
        }

        try {
            return generateInsights(context);
        } catch (Exception e) {
            System.err.println("AI Insights failed, using fallback: " + e.getMessage());
            return generateFallbackInsights(context);
        }
    }

    public String getChatResponse(String email, String query, Integer month, Integer year) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }

        AnalyticsResponse analytics = analyticsService.getAnalytics(email, month, year);

        FinancialDataContext context = new FinancialDataContext();
        context.setTotalIncome(analytics.getTotalIncome());
        context.setTotalExpense(analytics.getTotalExpense());
        context.setBalance(analytics.getBalance());
        context.setSavings(analytics.getSavings());
        context.setBudget(analytics.getBudget());
        context.setBudgetUsedPercentage(analytics.getBudgetUsedPercentage());
        context.setIncomeByCategory(analytics.getIncomeByCategory());
        context.setExpenseByCategory(analytics.getExpenseByCategory());
        context.setMonth(YearMonth.of(year, month).getMonth().name());
        context.setYear(String.valueOf(year));

        if (!ollamaService.isAvailable()) {
            return generateFallbackChatResponse(context, query);
        }

        try {
            return generateChatResponse(context, query);
        } catch (Exception e) {
            System.err.println("AI Chat failed, using fallback: " + e.getMessage());
            return generateFallbackChatResponse(context, query);
        }
    }

    private AIInsightResponse generateInsights(FinancialDataContext context) {
        String systemPrompt = "You are an expert financial advisor AI. Analyze the user's financial data and provide actionable insights, recommendations, and risk assessments. Be concise, practical, and encouraging.";
        String userPrompt = buildInsightPrompt(context);

        try {
            String aiResponse = ollamaService.generateCompletion(systemPrompt, userPrompt, 0.7, 1000);
            return parseInsightResponse(aiResponse, context);
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate AI insights: " + e.getMessage());
        }
    }

    private String generateChatResponse(FinancialDataContext context, String query) {
        String systemPrompt = "You are a helpful financial advisor assistant. Answer user questions about their finances based on the provided data. Be friendly, informative, and provide actionable advice. Keep responses concise and under 100 words.";
        String userPrompt = buildChatPrompt(context, query);

        try {
            return ollamaService.generateCompletion(systemPrompt, userPrompt, 0.7, 300);
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate AI response: " + e.getMessage());
        }
    }

    private String buildInsightPrompt(FinancialDataContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this financial data for ").append(context.getMonth())
              .append(" ").append(context.getYear()).append(":\n\n");
        
        prompt.append("Income: $").append(context.getTotalIncome()).append("\n");
        prompt.append("Expenses: $").append(context.getTotalExpense()).append("\n");
        prompt.append("Balance: $").append(context.getBalance()).append("\n");
        prompt.append("Savings: $").append(context.getSavings()).append("\n");
        
        if (context.getBudget() != null && context.getBudget().compareTo(BigDecimal.ZERO) > 0) {
            prompt.append("Budget: $").append(context.getBudget()).append("\n");
            prompt.append("Budget Used: ").append(context.getBudgetUsedPercentage()).append("%\n");
        }

        if (!context.getIncomeByCategory().isEmpty()) {
            prompt.append("\nIncome Categories: ");
            prompt.append(String.join(", ", context.getIncomeByCategory().keySet())).append("\n");
        }

        if (!context.getExpenseByCategory().isEmpty()) {
            prompt.append("Expense Categories: ");
            prompt.append(String.join(", ", context.getExpenseByCategory().keySet())).append("\n");
        }

        prompt.append("\nProvide a structured financial analysis with these exact sections:\n\n");
        prompt.append("INSIGHT:\n[2-3 sentence overall assessment]\n\n");
        prompt.append("RECOMMENDATIONS:\n");
        prompt.append("1. [First recommendation]\n");
        prompt.append("2. [Second recommendation]\n");
        prompt.append("3. [Third recommendation]\n\n");
        prompt.append("SPENDING_PATTERN:\n[Analysis of spending patterns]\n\n");
        prompt.append("SAVING_TIP:\n[One practical saving tip]\n\n");
        prompt.append("HEALTH_SCORE:\n[Number between 0-100]\n\n");
        prompt.append("RISK_ASSESSMENT:\n[Identify financial risks]\n");

        return prompt.toString();
    }

    private String buildChatPrompt(FinancialDataContext context, String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Financial Summary for ").append(context.getMonth()).append(":\n");
        prompt.append("Income: $").append(context.getTotalIncome()).append("\n");
        prompt.append("Expenses: $").append(context.getTotalExpense()).append("\n");
        prompt.append("Balance: $").append(context.getBalance()).append("\n");
        
        if (context.getBudget() != null && context.getBudget().compareTo(BigDecimal.ZERO) > 0) {
            prompt.append("Budget: $").append(context.getBudget()).append(" (")
                   .append(String.format("%.1f", context.getBudgetUsedPercentage())).append("% used)\n");
        }

        if (!context.getExpenseByCategory().isEmpty()) {
            prompt.append("\nTop Expense Categories: ");
            prompt.append(String.join(", ", context.getExpenseByCategory().keySet().stream()
                    .limit(3).toArray(String[]::new))).append("\n");
        }

        prompt.append("\nUser Question: ").append(query).append("\n\n");
        prompt.append("Provide a helpful, concise answer (under 100 words) based on the financial data above.");

        return prompt.toString();
    }

    // Keep all your fallback methods exactly as they are
    private AIInsightResponse generateFallbackInsights(FinancialDataContext context) {
        AIInsightResponse response = new AIInsightResponse();
        Double healthScore = calculateBasicHealthScore(context);
        response.setFinancialHealthScore(healthScore);
        response.setInsight(generateBasicInsight(context, healthScore));
        response.setRecommendations(generateBasicRecommendations(context));
        response.setSpendingPattern(analyzeSpendingPattern(context));
        response.setSavingTip(generateSavingTip(context));
        response.setRiskAssessment(assessRisks(context));
        return response;
    }

    private String generateFallbackChatResponse(FinancialDataContext context, String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("expense") || lowerQuery.contains("spending")) {
            return generateExpenseAdvice(context);
        } else if (lowerQuery.contains("save") || lowerQuery.contains("saving")) {
            return generateSavingAdvice(context);
        } else if (lowerQuery.contains("budget")) {
            return generateBudgetAdvice(context);
        } else if (lowerQuery.contains("income")) {
            return generateIncomeAdvice(context);
        } else {
            return generateGeneralAdvice(context);
        }
    }

    private String generateBasicInsight(FinancialDataContext context, Double healthScore) {
        StringBuilder insight = new StringBuilder();
        if (healthScore >= 80) {
            insight.append("Excellent financial health! ");
        } else if (healthScore >= 60) {
            insight.append("Good financial management with room for improvement. ");
        } else {
            insight.append("Your finances need attention. ");
        }

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (context.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = context.getSavings()
                    .divide(context.getTotalIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            insight.append("You're saving an impressive ").append(savingsRate.intValue()).append("% of your income. ");
        } else if (savingsRate.compareTo(BigDecimal.ZERO) > 0) {
            insight.append("You're currently saving ").append(savingsRate.intValue()).append("% of your income. ");
        } else {
            insight.append("Consider increasing your savings rate. ");
        }

        if (context.getBudgetUsedPercentage() != null) {
            if (context.getBudgetUsedPercentage() > 100) {
                insight.append("Alert: You've exceeded your budget by ")
                       .append(String.format("%.1f", context.getBudgetUsedPercentage() - 100)).append("%.");
            } else if (context.getBudgetUsedPercentage() > 80) {
                insight.append("You're approaching your budget limit.");
            }
        }
        return insight.toString();
    }

    private List<String> generateBasicRecommendations(FinancialDataContext context) {
        List<String> recommendations = new ArrayList<>();
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (context.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = context.getSavings()
                    .divide(context.getTotalIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        if (savingsRate.compareTo(BigDecimal.valueOf(20)) < 0) {
            recommendations.add("Aim to save at least 20% of your income each month");
        }
        if (context.getBudget() == null || context.getBudget().compareTo(BigDecimal.ZERO) == 0) {
            recommendations.add("Set a monthly budget to better control your spending");
        } else if (context.getBudgetUsedPercentage() > 90) {
            recommendations.add("Review and reduce expenses in your top spending categories");
        }
        if (!context.getExpenseByCategory().isEmpty()) {
            Map.Entry<String, BigDecimal> topExpense = context.getExpenseByCategory().entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (topExpense != null) {
                recommendations.add("Your highest expense is " + topExpense.getKey() + " - look for ways to optimize this category");
            }
        }
        if (context.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            recommendations.add("Focus on reducing expenses to achieve a positive balance");
        }
        recommendations.add("Track all expenses daily to identify unnecessary spending");
        recommendations.add("Build an emergency fund covering 3-6 months of expenses");
        return recommendations;
    }

    private String analyzeSpendingPattern(FinancialDataContext context) {
        if (context.getExpenseByCategory().isEmpty()) {
            return "No expense data available for analysis. Start tracking your expenses to see patterns.";
        }
        StringBuilder pattern = new StringBuilder("Your spending is distributed across ");
        pattern.append(context.getExpenseByCategory().size()).append(" categories. ");
        List<Map.Entry<String, BigDecimal>> sortedExpenses = new ArrayList<>(context.getExpenseByCategory().entrySet());
        sortedExpenses.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());
        if (!sortedExpenses.isEmpty()) {
            pattern.append("Top categories: ");
            int count = Math.min(3, sortedExpenses.size());
            for (int i = 0; i < count; i++) {
                pattern.append(sortedExpenses.get(i).getKey());
                if (i < count - 1) pattern.append(", ");
            }
            pattern.append(". ");
        }
        if (context.getTotalExpense().compareTo(context.getTotalIncome()) > 0) {
            pattern.append("Your expenses exceed income - immediate action needed.");
        } else {
            pattern.append("Your spending is within your income range.");
        }
        return pattern.toString();
    }

    private String generateSavingTip(FinancialDataContext context) {
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (context.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = context.getSavings()
                    .divide(context.getTotalIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        if (savingsRate.compareTo(BigDecimal.valueOf(10)) < 0) {
            return "Start with the 50-30-20 rule: 50% needs, 30% wants, 20% savings. Even small amounts add up over time!";
        } else if (savingsRate.compareTo(BigDecimal.valueOf(20)) < 0) {
            return "You're doing well! Try to increase your savings rate to 20% by reducing discretionary spending.";
        } else {
            return "Excellent savings rate! Consider diversifying into investments for long-term growth.";
        }
    }

    private String assessRisks(FinancialDataContext context) {
        List<String> risks = new ArrayList<>();
        if (context.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            risks.add("Negative balance indicates overspending");
        }
        if (context.getBudgetUsedPercentage() != null && context.getBudgetUsedPercentage() > 100) {
            risks.add("Budget exceeded - risk of debt accumulation");
        }
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (context.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = context.getSavings()
                    .divide(context.getTotalIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        if (savingsRate.compareTo(BigDecimal.valueOf(5)) < 0) {
            risks.add("Low savings rate - vulnerable to emergencies");
        }
        if (risks.isEmpty()) {
            return "No major financial risks detected. Continue monitoring your finances regularly.";
        }
        return String.join(". ", risks) + ". Take corrective action promptly.";
    }

    private String generateExpenseAdvice(FinancialDataContext context) {
        return String.format("Your total expenses for %s are $%.2f. Consider reviewing your top spending categories and identifying areas to cut back. Small reductions in multiple categories can lead to significant savings.", context.getMonth(), context.getTotalExpense());
    }

    private String generateSavingAdvice(FinancialDataContext context) {
        return String.format("You've saved $%.2f this month. To increase savings, try automating transfers to a savings account right after receiving income. The 50-30-20 budgeting rule can help: 50%% needs, 30%% wants, 20%% savings.", context.getSavings());
    }

    private String generateBudgetAdvice(FinancialDataContext context) {
        if (context.getBudget() == null || context.getBudget().compareTo(BigDecimal.ZERO) == 0) {
            return "You haven't set a budget yet. Start by tracking your average monthly expenses for 2-3 months, then set a realistic budget slightly below that amount to create room for savings.";
        }
        return String.format("Your budget is $%.2f and you've used %.1f%% of it. %s", context.getBudget(), context.getBudgetUsedPercentage(), context.getBudgetUsedPercentage() > 90 ? "You're close to your limit - be cautious with remaining expenses." : "You're on track - keep monitoring to stay within budget.");
    }

    private String generateIncomeAdvice(FinancialDataContext context) {
        return String.format("Your income for %s is $%.2f. Consider diversifying income sources through side projects, investments, or skill development. Multiple income streams provide financial security.", context.getMonth(), context.getTotalIncome());
    }

    private String generateGeneralAdvice(FinancialDataContext context) {
        return String.format("For %s: Income $%.2f, Expenses $%.2f, Balance $%.2f. Focus on maintaining a positive balance, tracking all expenses, and building an emergency fund. Regular financial reviews help identify improvement opportunities.", context.getMonth(), context.getTotalIncome(), context.getTotalExpense(), context.getBalance());
    }

    private AIInsightResponse parseInsightResponse(String aiResponse, FinancialDataContext context) {  // Fixed: Was AlInsightResponse
        AIInsightResponse response = new AIInsightResponse();
        try {
            String[] sections = aiResponse.split("\n\n");
            for (String section : sections) {
                String upperSection = section.toUpperCase();
                if (upperSection.contains("INSIGHT:")) {
                    response.setInsight(extractContent(section, "INSIGHT:"));
                } else if (upperSection.contains("RECOMMENDATIONS:") || upperSection.contains("RECOMMENDATION:")) {
                    response.setRecommendations(extractListItems(section));
                } else if (upperSection.contains("SPENDING_PATTERN:") || upperSection.contains("SPENDING PATTERN:")) {
                    response.setSpendingPattern(extractContent(section, "SPENDING_PATTERN:"));
                } else if (upperSection.contains("SAVING_TIP:") || upperSection.contains("SAVING TIP:")) {
                    response.setSavingTip(extractContent(section, "SAVING_TIP:"));
                } else if (upperSection.contains("HEALTH_SCORE:") || upperSection.contains("HEALTH SCORE:")) {
                    response.setFinancialHealthScore(extractScore(section));
                } else if (upperSection.contains("RISK_ASSESSMENT:") || upperSection.contains("RISK ASSESSMENT:")) {
                    response.setRiskAssessment(extractContent(section, "RISK_ASSESSMENT:"));
                }
            }
            if (response.getInsight() == null || response.getInsight().trim().isEmpty()) {
                response.setInsight(aiResponse.substring(0, Math.min(300, aiResponse.length())));
            }
            if (response.getRecommendations() == null || response.getRecommendations().isEmpty()) {
                response.setRecommendations(generateBasicRecommendations(context));
            }
            if (response.getFinancialHealthScore() == null) {
                response.setFinancialHealthScore(calculateBasicHealthScore(context));
            }
            if (response.getSpendingPattern() == null || response.getSpendingPattern().trim().isEmpty()) {
                response.setSpendingPattern(analyzeSpendingPattern(context));
            }
            if (response.getSavingTip() == null || response.getSavingTip().trim().isEmpty()) {
                response.setSavingTip(generateSavingTip(context));
            }
            if (response.getRiskAssessment() == null || response.getRiskAssessment().trim().isEmpty()) {
                response.setRiskAssessment(assessRisks(context));
            }
        } catch (Exception e) {
            response.setInsight(aiResponse);
            response.setRecommendations(generateBasicRecommendations(context));
            response.setFinancialHealthScore(calculateBasicHealthScore(context));
            response.setSpendingPattern(analyzeSpendingPattern(context));
            response.setSavingTip(generateSavingTip(context));
            response.setRiskAssessment(assessRisks(context));
        }
        return response;
    }

    private String extractContent(String section, String marker) {
        int startIndex = section.toUpperCase().indexOf(marker.toUpperCase());
        if (startIndex == -1) return section.trim();
        String content = section.substring(startIndex + marker.length()).trim();
        content = content.replaceAll("^[0-9]+\\.\\s*", "").trim();
        content = content.replaceAll("(?i)" + marker.replace(":", ""), "").trim();
        return content;
    }

    private List<String> extractListItems(String section) {
        List<String> items = new ArrayList<>();
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^[0-9]+\\..*") || line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                String item = line.replaceAll("^[0-9]+\\.\\s*", "").replaceAll("^[-•*]\\s*", "").trim();
                if (!item.isEmpty() && !item.toUpperCase().contains("RECOMMENDATION") && item.length() > 10) {
                    items.add(item);
                }
            }
        }
        return items.isEmpty() ? generateBasicRecommendations(null) : items;
    }

    private Double extractScore(String section) {
        try {
            String numberStr = section.replaceAll("[^0-9.]", "");
            double score = Double.parseDouble(numberStr);
            return Math.max(0, Math.min(100, score));
        } catch (Exception e) {
            return 75.0;
        }
    }

    private Double calculateBasicHealthScore(FinancialDataContext context) {
        double score = 50.0;
        if (context.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            score += 20;
        }
        if (context.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingsRatio = context.getSavings()
                    .divide(context.getTotalIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (savingsRatio.compareTo(BigDecimal.valueOf(20)) >= 0) {
                score += 20;
            } else if (savingsRatio.compareTo(BigDecimal.valueOf(10)) >= 0) {
                score += 10;
            }
        }
        if (context.getBudgetUsedPercentage() != null) {
            if (context.getBudgetUsedPercentage() <= 80) {
                score += 10;
            } else if (context.getBudgetUsedPercentage() > 100) {
                score -= 10;
            }
        }
        return Math.max(0, Math.min(100, score));
    }
}