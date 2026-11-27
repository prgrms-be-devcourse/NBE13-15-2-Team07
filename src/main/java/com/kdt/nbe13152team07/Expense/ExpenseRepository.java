package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.Participation.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface ExpenseRepository extends JpaRepository<Expense,Long> {
    Boolean existsByPaymentTxId(String Id);
    @Query(value = """
        select e from Expense e
        join fetch e.categoryId c
        join e.participationId p
        where p.user.id = :userId
          and e.spentAt >= :start and e.spentAt <= :end
          and (:categoryId is null or c.id = :categoryId)
          and e.canceledAt is null
        order by e.spentAt desc
        """,
            countQuery = """
        select count(e) from Expense e
        join e.participationId p
        where p.user.id = :userId
          and e.spentAt >= :start and e.spentAt <= :end
          and (:categoryId is null or e.categoryId.id = :categoryId)
          and e.canceledAt is null
        """)
    Page<Expense> findMyExpenses(@Param("userId") Long userId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("categoryId") Long categoryId,
                                 Pageable pageable);
    // 날짜 범위(시작일 ~ 종료일, 둘 다 포함)로 조회
    default Page<Expense> findMyExpensesByDate(Long userId, LocalDate startDate, LocalDate endDate,
                                               Long categoryId, Pageable pageable) {
        return findMyExpenses(userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                categoryId,
                pageable);
    }

}
