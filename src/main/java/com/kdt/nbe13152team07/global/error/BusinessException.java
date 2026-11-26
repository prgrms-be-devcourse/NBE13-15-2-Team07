package com.kdt.nbe13152team07.global.error;

import lombok.Getter;

// 서비스에서 규칙 위반을 알리는 공통 예외
// GlobalExceptionHandler가 잡아서 ErrorCode 기준으로 응답함
// RuntimeException 상속 -> throws 선언이 필요 없음, @Transactional 롤백 대상이 됨
@Getter
public class BusinessException extends RuntimeException {

    // 응답 상태와 메시지를 정하는 기준
    private final ErrorCode errorCode;

    // 사용법 예시 : throw new BusinessException(ErrorCode.ROOM_FULL);
    public BusinessException(ErrorCode errorCode) {
        // 부모에게 메시지를 넘겨 로그 및 스택 트레이스에 오류 문구가 보이게 함
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
