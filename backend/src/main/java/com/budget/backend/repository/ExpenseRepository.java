package com.budget.backend.repository;


import com.budget.backend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdOrderByDateDesc(Long userId);
    
    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);
    
    List<Expense> findByUserIdAndCategoryOrderByDateDesc(Long userId, String category);
    
    List<Expense> findByUserIdAndDateBetweenAndCategoryOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate, String category);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal getTotalExpenseByUserId(Long userId);
    
    List<Expense> findTop5ByUserIdOrderByDateDesc(Long userId);
}
