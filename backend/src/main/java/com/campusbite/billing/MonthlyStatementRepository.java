package com.campusbite.billing;

import com.campusbite.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyStatementRepository extends JpaRepository<MonthlyStatement, Long> {
    boolean existsByUserAndYearAndMonth(User user, int year, int month);
    List<MonthlyStatement> findByUserOrderByYearDescMonthDesc(User user);
    List<MonthlyStatement> findBySettledFalseOrderByYearAscMonthAsc();
}