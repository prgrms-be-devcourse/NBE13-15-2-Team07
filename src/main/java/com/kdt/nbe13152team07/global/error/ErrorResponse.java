package com.kdt.nbe13152team07.global.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ErrorResponse(
    String code,
    String message,
    Map<String, String> fieldErrors
) {

    // compact constructor(record에서 명시적인 매개변수 목록을 가지지 않는 생성자)
    // 어떤 경로로 만들어도 fieldErrors가 null이 되지 않게 함
    public ErrorResponse {
        fieldErrors = (fieldErrors == null)     // 넘어온 fieldErrors가 null인지 확인
                ? Map.of()                      // null이면 비어 있는 수정 불가 Map으로 바꿈 -> JSON으로는 {}가 됨
                // null이 아니면 넣은 순서를 지키며 복사(LinkedHashMap)하고, 수정할 수 없게 감쌈(unmodifiableMap) -> record의 불변성을 지키기 위해서
                : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    // 필드 오류가 없는 응답
    public static ErrorResponse of(ErrorCode errorCode) {
        // fieldErrors에 null을 넘겨서 생성자에서 {}로 변환되게끔 함
        return of(errorCode, null);
    }
    
    // 필드 오류 메시지가 있는 응답
    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }

}
