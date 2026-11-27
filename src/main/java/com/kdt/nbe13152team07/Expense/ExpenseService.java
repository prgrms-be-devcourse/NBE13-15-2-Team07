package com.kdt.nbe13152team07.Expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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






}
