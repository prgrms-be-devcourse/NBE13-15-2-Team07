package com.kdt.mock_pay.global.error;

import lombok.Getter;

//RuntimeException -> throws 선언이 필요 없습니다. 메서드마다 예외를 선언하지 않아도 됨
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); //로그 오류 문구가 보이도록 부모에게 전달
        this.errorCode = errorCode;
    }
}
