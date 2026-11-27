package com.kdt.mock_pay.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //400 : 입력값 형식 범위 오류, JSON 문법 오류, 파라미터 타입 오류
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),

    //404
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."), //[명세 외] 없는 URL
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다"), //없는 핀테크이용번호
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."), //없는 거래 ID

    //405 : [명세 외] 경로는 있는데 지원하지 않는 HTTP 메서드
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),

    //409 : 거절된 결제 승인, 대기 중인 결제 취소처럼 상태가 맞지 않음
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "지금 결제 상태에서는 처리할 수 없습니다."),

    //500 : [명세 외] 처리하지 못한 예외
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    //502 : M5에서 우리 서버로 취소(X2) 전달 실패 -> 결제는 승인 상태 유지
    CANCEL_NOT_DELIVERED(HttpStatus.BAD_GATEWAY, "가맹점에 취소를 전달하지 못해 승인 상태를 유지합니다.");

    private final HttpStatus status;
    private final String message;
}
