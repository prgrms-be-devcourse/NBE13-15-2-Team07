package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Categories.CategoriesRepository;
import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.global.common.PageResponse;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import com.kdt.nbe13152team07.room.Room;
import com.kdt.nbe13152team07.room.RoomStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ParticipationRepository participationRepository;
    private final CategoriesRepository categoriesRepository;
    private final Clock clock;


    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository,ParticipationRepository participationRepository,CategoriesRepository categoriesRepository,Clock clock){
        this.expenseRepository=expenseRepository;
        this.participationRepository=participationRepository;
        this.categoriesRepository=categoriesRepository;
        this.clock=clock;
    }


    @Transactional
    public Long recordPayment(RecordPaymentCommand command) {
        //이미 처리된 주문일 경우, 그 주문의 if를 반환
        if(expenseRepository.existsByPaymentTxId(command.paymentTxId())){
            return expenseRepository.findByPaymentTxId(command.paymentTxId()).getId();
        }
        //새 주문 처리 생성
        Participation participation=participationRepository.findByActiveUserId(command.participationId()).orElseThrow(()->new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));
        RoomStatus status=participation.getRoom().getStatus();
        //방이 챌린지를 진행 중에만 기록을 남김
        if(status.equals(RoomStatus.IN_PROGRESS)) {
            Categories categories = categoriesRepository.findById(command.categoryId()).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            Expense expense = new Expense(participation, categories, command.amount(), command.merchantName(), command.spentAt().atZone(clock.getZone()).toOffsetDateTime(), command.paymentTxId());
            return expenseRepository.save(expense).getId();
        }
        else{
            //방이 시작을 안하면 기록을 남기지 않음. 만약 이 오류로 인해 결제 서버에서 재전송을 하는 로직일 시, 삭제 필수
            throw new BusinessException(ErrorCode.ROOM_NOT_STARTED);
        }
    }

    // TODO 임시: 재영님 구현으로 교체 예정 (결제 연동 X2가 호출)
    // txId로 지출을 찾아 canceled_at 기록. 없으면 NOT_FOUND, 정산 끝난 방이면 ALREADY_SETTLED
    public ExpenseCancelResult cancelByPaymentTx(String paymentTxId) {
        if(!expenseRepository.existsByPaymentTxId(paymentTxId))
            return ExpenseCancelResult.NOT_FOUND;
        else {
            Expense expense = expenseRepository.findByPaymentTxId(paymentTxId);
            Room room=expense.getParticipationId().getRoom();
            if(room.getStatus().equals(RoomStatus.IN_PROGRESS)){
                expense.setCanceledAt(OffsetDateTime.now(clock));
                expenseRepository.save(expense);
                return ExpenseCancelResult.CANCELED;
            }
            else{
                //앞으로 구현 예정
                return ExpenseCancelResult.ALREADY_SETTLED;
            }
        }


    }

   @Transactional(readOnly = true)
   public PageResponse<ExpenseResponse> findMyExpenses(Long userId, ExpenseViewRequest request, int page, int size) {
       Page<ExpenseResponse> result = expenseRepository
               .findMyExpensesByDate(userId ,request.start(), request.end(),clock.getZone(), request.categoryId(), PageRequest.of(page, Math.min(size, 100)))
               .map(this::toResponse);

       return PageResponse.from(result);
   }
    private ExpenseResponse toResponse(Expense expense){
        return new ExpenseResponse(expense.getId(),expense.getCategoryId().getId(),expense.getCategoryId().getName(),expense.getAmount(),expense.getMerchantName(),expense.getSpentAt(),expense.getCategoryId().getExcludable());
    }





}
