package com.kdt.nbe13152team07.payment.service;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Expense.ExpenseCancelResult;
import com.kdt.nbe13152team07.Expense.ExpenseService;
import com.kdt.nbe13152team07.Expense.RecordPaymentCommand;
import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import com.kdt.nbe13152team07.payment.dto.PaymentApprovedRequest;
import com.kdt.nbe13152team07.payment.dto.PaymentReceiptResponse;
import com.kdt.nbe13152team07.payment.dto.ReceiptResult;
import com.kdt.nbe13152team07.payment.entity.PaymentLink;
import com.kdt.nbe13152team07.payment.entity.PaymentLinkStatus;
import com.kdt.nbe13152team07.payment.repository.PaymentLinkRepository;
import com.kdt.nbe13152team07.payment.repository.PaymentReceiptStore;
import com.kdt.nbe13152team07.payment.repository.PaymentReceiptStore.Type;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import com.kdt.nbe13152team07.room.Room;
import com.kdt.nbe13152team07.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PaymentReceiveService {

    private final PaymentReceiptStore receiptStore;
    private final PaymentLinkRepository paymentLinkRepository;
    private final ParticipationRepository participationRepository;
    private final CategoryClassifier categoryClassifier;
    private final ExpenseService expenseService;

    public PaymentReceiveService(PaymentReceiptStore receiptStore,
                                 PaymentLinkRepository paymentLinkRepository,
                                 ParticipationRepository participationRepository,
                                 CategoryClassifier categoryClassifier,
                                 ExpenseService expenseService) {
        this.receiptStore = receiptStore;
        this.paymentLinkRepository = paymentLinkRepository;
        this.participationRepository = participationRepository;
        this.categoryClassifier = categoryClassifier;
        this.expenseService = expenseService;
    }

    // X1 승인 결제 수신
    @Transactional
    public PaymentReceiptResponse receive(PaymentApprovedRequest req) {
        String txId = req.transactionId();

        // ② 중복 확인: 이미 들어온 결제면 저장된 결과, 아직 처리 중이면 DUPLICATE
        if (!receiptStore.tryStart(Type.APPROVE, txId)) {
            return receiptStore.findResult(Type.APPROVE, txId)
                    .orElseGet(() -> PaymentReceiptResponse.ignored(txId, ReceiptResult.DUPLICATE));
        }

        try {
            // ③ 통장 → 사용자
            Optional<PaymentLink> link = paymentLinkRepository
                    .findByFintechUseNumAndStatus(req.fintechUseNum(), PaymentLinkStatus.ACTIVE);
            if (link.isEmpty()) {
                return save(txId, PaymentReceiptResponse.ignored(txId, ReceiptResult.IGNORED_NOT_LINKED));
            }
            Long userId = link.get().getUser().getId();

            // ④ 사용자 → 진행 중 참여 + 결제 시각이 방 기간 안인지
            Optional<Participation> participation = participationRepository.findByActiveUserId(userId)
                    .filter(p -> isRecordable(p, req.approvedAt().toLocalDate()));
            if (participation.isEmpty()) {
                return save(txId, PaymentReceiptResponse.ignored(txId, ReceiptResult.IGNORED_NO_ACTIVE_CHALLENGE));
            }
            Participation p = participation.get();

            // ⑤ 업종코드 → 카테고리
            Categories category = categoryClassifier.classify(req.merchantCategoryCode());

            // ⑥ 지출 저장 요청 (3번 담당)
            Long expenseId = expenseService.recordPayment(new RecordPaymentCommand(
                    p.getId(),
                    category.getId(),
                    Math.toIntExact(req.amount()),
                    req.merchantName(),
                    req.approvedAt(),
                    txId
            ));

            // ⑦ 제외 카테고리인지 계산해서 응답
            boolean excluded = p.getRoom().getExcludedCategoryIds().contains(category.getId());
            return save(txId, PaymentReceiptResponse.recorded(txId, expenseId, category.getCode(), excluded));

        } catch (RuntimeException e) {
            receiptStore.release(Type.APPROVE, txId);   // 깃발을 지워야 mock-pay 재시도가 다시 처리됨
            throw e;
        }
    }

    // X2 승인 취소 수신
    // 통장·참여·카테고리는 승인 때 이미 확인했으므로 txId만으로 지출 쪽에 취소를 맡긴다
    @Transactional
    public PaymentReceiptResponse cancel(String txId) {

        // ① 중복 확인: 승인과 다른 키(payment:cancel:)를 써서 승인 결과에 막히지 않음
        if (!receiptStore.tryStart(Type.CANCEL, txId)) {
            return receiptStore.findResult(Type.CANCEL, txId)
                    .orElseGet(() -> PaymentReceiptResponse.ignored(txId, ReceiptResult.DUPLICATE));
        }

        try {
            // ② 지출 취소 요청 (3번 담당: canceled_at 기록)
            ExpenseCancelResult result = expenseService.cancelByPaymentTx(txId);

            // ③ 결과 → 응답 코드
            PaymentReceiptResponse response = switch (result) {
                // 승인 때 무시돼서 지출이 없는 결제도 mock-pay 입장에서는 취소 완료로 응답
                case CANCELED, NOT_FOUND -> PaymentReceiptResponse.canceled(txId);
                case ALREADY_SETTLED -> PaymentReceiptResponse.ignored(txId, ReceiptResult.IGNORED_ALREADY_SETTLED);
            };
            receiptStore.saveResult(Type.CANCEL, txId, response);
            return response;

        } catch (RuntimeException e) {
            receiptStore.release(Type.CANCEL, txId);   // 깃발을 지워야 mock-pay 재시도가 다시 처리됨
            throw e;
        }
    }

    // 참여 상태 ACTIVE + 방이 진행 중 + 결제일이 시작일~종료일 사이
    private boolean isRecordable(Participation p, LocalDate paidDate) {
        Room room = p.getRoom();
        return p.getStatus() == ParticipationStatus.ACTIVE
                && room.getStatus() == RoomStatus.IN_PROGRESS
                && !paidDate.isBefore(room.getStartDate())
                && !paidDate.isAfter(room.getEndDate());
    }

    private PaymentReceiptResponse save(String txId, PaymentReceiptResponse response) {
        receiptStore.saveResult(Type.APPROVE, txId, response);
        return response;
    }
}
