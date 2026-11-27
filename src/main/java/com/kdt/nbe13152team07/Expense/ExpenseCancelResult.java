package com.kdt.nbe13152team07.Expense;

// ExpenseService.cancelByPaymentTx 결과 (결제 연동 X2가 응답 코드를 정할 때 사용)
// 예외 대신 결과로 돌려줘야 호출하는 쪽 트랜잭션이 롤백 전용으로 바뀌지 않는다
// TODO 임시: 재영님과 확정 후 교체 가능
public enum ExpenseCancelResult {
    CANCELED,         // canceled_at 기록 완료 (이미 취소된 건이어도 CANCELED)
    NOT_FOUND,        // 해당 txId 지출이 없음 (승인 때 무시된 결제)
    ALREADY_SETTLED   // 정산이 끝난 방의 지출이라 취소하지 않음
}
