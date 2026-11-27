package com.kdt.mock_pay.global.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 오류가 발생했을 때 클라이언트에 내려주는 공통 응답 본문.
 *
 * <p>예시:
 * <pre>
 * {
 *   "code": "INVALID_INPUT",
 *   "message": "입력값이 올바르지 않습니다.",
 *   "fieldErrors": { "amount": "1 이상이어야 합니다." }
 * }
 * </pre>
 *
 * @param code        오류 종류를 구분하는 코드 ({@link ErrorCode}의 이름)
 * @param message     사용자에게 보여줄 오류 메시지 ({@link ErrorCode}의 메시지)
 * @param fieldErrors 입력값 검증 실패 시 필드명과 사유 (없으면 빈 맵)
 */
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {

    /**
     * fieldErrors가 null이면 빈 맵으로 바꾸고,
     * 값이 있으면 입력 순서를 유지한 채 수정할 수 없는 복사본으로 저장한다.
     */
    public ErrorResponse{
        fieldErrors = (fieldErrors == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** 필드 오류 없이 {@link ErrorCode}만으로 응답을 만든다. */
    public static ErrorResponse of(ErrorCode errorCode){
        return of(errorCode, null);
    }

    /** {@link ErrorCode}와 필드별 검증 오류를 함께 담아 응답을 만든다. */
    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fieldErrors){
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }

}

