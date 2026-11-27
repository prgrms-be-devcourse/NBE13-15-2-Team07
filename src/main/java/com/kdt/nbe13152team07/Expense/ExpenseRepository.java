package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.Participation.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ExpenseRepository extends JpaRepository<Expense,Long> {
    Boolean existsByPaymentTxId(String Id);
    @Query(value= """
    select e from Expense e join fetch e.categoryId c
    where e.participationId= :participation
    and e.spentAt >= :start and e.spentAt< :end
    and (:categoryId is null or c.id= :categoryId)
    and e.canceledAt is null
    order by e.spentAt desc
""",
            countQuery = """
select count(e) from Expense e where e.participationId = :participation
and e.spentAt >= :start and e.spentAt< :end
and(:categoryId is null or e.categoryId.id = :categoryId)
and e.canceledAt is null
"""
    )
    Page<Expense> findByParticipationAndPeriod(@Param("participation") Participation participation,
                                               @Param("start")LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("categoryId") Long categoryId,
                                               Pageable pageable);
}
