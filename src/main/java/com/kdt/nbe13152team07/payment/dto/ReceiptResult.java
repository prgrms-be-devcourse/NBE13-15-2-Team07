package com.kdt.nbe13152team07.payment.dto;

public enum ReceiptResult {
    RECORDED,
    DUPLICATE,
    CANCELED,
    IGNORED_NO_ACTIVE_CHALLENGE,
    IGNORED_NOT_LINKED,
    IGNORED_ALREADY_SETTLED
}