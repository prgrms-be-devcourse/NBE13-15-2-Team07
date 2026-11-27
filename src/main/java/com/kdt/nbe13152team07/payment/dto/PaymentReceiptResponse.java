package com.kdt.nbe13152team07.payment.dto;

public record PaymentReceiptResponse(
        String transactionId,
        ReceiptResult result,
        Long expenseId,
        String category,
        Boolean excluded
) {
    public static PaymentReceiptResponse recorded(String transactionId, Long expenseId,
                                                  String category, boolean excluded) {
        return new PaymentReceiptResponse(
                transactionId, ReceiptResult.RECORDED, expenseId, category, excluded
        );
    }

    // X2 취소 완료
    public static PaymentReceiptResponse canceled(String transactionId) {
        return new PaymentReceiptResponse(
                transactionId, ReceiptResult.CANCELED, null, null, null
        );
    }

    //서비스에서 마저 작성해야 함
    public static PaymentReceiptResponse ignored(String transactionId, ReceiptResult result) {
        return new PaymentReceiptResponse(
                transactionId, result, null, null, null
        );
    }
}
