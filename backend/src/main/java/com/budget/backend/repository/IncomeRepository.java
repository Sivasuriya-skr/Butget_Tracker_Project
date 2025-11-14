
package com.budget.backend.repository;

import com.budget.backend.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserIdOrderByDateDesc(Long userId);
    
    List<Income> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);
    
    List<Income> findByUserIdAndCategoryOrderByDateDesc(Long userId, String category);
    
    List<Income> findByUserIdAndDateBetweenAndCategoryOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate, String category);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId")
    BigDecimal getTotalIncomeByUserId(Long userId);
    
    List<Income> findTop5ByUserIdOrderByDateDesc(Long userId);
}