package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.global.common.PageResponse;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import com.kdt.nbe13152team07.room.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;


    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository){
        this.expenseRepository=expenseRepository;

    }

    // TODO 임시: 재영님 구현으로 교체 예정
    public Long recordPayment(RecordPaymentCommand command) {
        throw new UnsupportedOperationException("구현 예정");
    }

   /*
   나중에 mockpay와 연동되는 dto를 이용해 구현 예정
   */
   @Transactional(readOnly = true)
   public PageResponse<ExpenseResponse> findMyExpenses(Long userId, ExpenseViewRequest request, int page, int size) {
       Page<ExpenseResponse> result = expenseRepository
               .findMyExpensesByDate(userId ,request.start(), request.end(), request.categoryId(), PageRequest.of(page, Math.min(size, 100)))
               .map(this::toResponse);

       return PageResponse.from(result);
   }
    private ExpenseResponse toResponse(Expense expense){
        return new ExpenseResponse(expense.getId(),expense.getCategoryId().getId(),expense.getCategoryId().getName(),expense.getAmount(),expense.getMerchantName(),expense.getSpentAt(),expense.getCategoryId().getExcludable());
    }





}
