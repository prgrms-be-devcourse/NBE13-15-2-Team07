package com.kdt.nbe13152team07.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

// 모든 컨트롤러에서 던진 예외를 한곳에서 잡아 {code, message, fieldErrors} 형식으로 응답
// 필터에서 걸러지는 Exception은 여기로 오지 않음 (Jwt 인증 실패, 권한 부족 등)
@RestControllerAdvice
// log 필드 자동 생성
@Slf4j
public class GlobalExceptionHandler {

    // 응답 생성 공통 메서드 - fieldErrors가 없음
    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 응답 생성 공통 메서드 - fieldErrors가 있음
    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, fieldErrors));
    }
    
    // 아래서부터 Handling ---------------------------------------------------------------------------------------------

    // 서비스 규칙 위반 ( 예 : throw new BusinessException(ErrorCode.ROOM_FULL) )
    // 상태 및 메시지는 ErrorCode가 정함, fieldErrors는 {}
    // 정상적인 사용자 오류라 debug 수준으로 한 줄만 남김
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.debug("BusinessException: {}", e.getMessage());
        return toResponse(e.getErrorCode());
    }

    // @Valid @RequestBody 검증 실패 ( 예 : 닉네임 1자, 필수값 누락 등 )
    // 400 VALIDATION_ERROR, fieldErrors = {필드 이름: 어노테이션에 적은 메시지}
    // 적은 메시지가 없으면 기본 메시지가 들어감
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        // LinkedHashMap -> 오류가 발생한 순서대로 응답에 나가게 함
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        // getBindingResult() : 검증 결과를 담은 상자, getFieldErrors() : 그중 필드 단위 오류 목록
        for(FieldError error : e.getBindingResult().getFieldErrors()) {
            // putIfAbsent : 필드당 메시지는 하나만 나갈 수 있도록
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return toResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    // @RequestParam, @PathVariable 등 파라미터 검증 실패 ( 예 : @PathVariable @Positive Long roomId에 -1이 들어감 )
    // 400 VALIDATION_ERROR, fieldErrors = {파라미터 이름 : 메시지}
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e) {
        // 결과를 담을 Map, 넣은 순서대로 응답에 나감
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        // 검증에 실패한 파라미터마다 결과를 하나씩 꺼내 처리
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            // 같은 메서드에 @Valid 본문이 함께 있으면 본문 오류도 여기로 옴 -> 필드 단위로 꺼냄
            if (result instanceof ParameterErrors errors) {
                for (FieldError error : errors.getFieldErrors()) {
                    fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
                }
                continue;
            }
            // 결과가 어느 파라미터의 것인지를 찾아서 그 파라미터의 변수 이름을 가져옴
            String name = result.getMethodParameter().getParameterName();
            // 이 파라미터에서 실패한 제약 목록을 가져와서 첫 번째만 씀
            String message = result.getResolvableErrors().get(0).getDefaultMessage();
            fieldErrors.putIfAbsent(name, message);
        }
        return toResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    // 파라미터 타입 변환 실패 ( 예: ?page=abc, /rooms/abc, enum에 없는 값 )
    // 400 VALIDATION_ERROR, fieldErrors = {파라미터 이름 : "형식이 올바르지 않습니다."}
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return toResponse(ErrorCode.VALIDATION_ERROR, Map.of(e.getName(), "형식이 올바르지 않습니다."));
    }

    // 필수 @RequestParam 누락 ( 예 : required = true인데 쿼리 파라미터 없음 )
    // 400 VALIDATION_ERROR, fieldErrors = {파라미터 이름 : "필수 값입니다."}
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return toResponse(ErrorCode.VALIDATION_ERROR, Map.of(e.getParameterName(), "필수 값입니다."));
    }

    // 본문을 읽을 수 없음(JSON 문법 오류, 본문 없음, 숫자 자리에 문자 등 본문 필드 타입 오류), Content-Type이 JSON이 아님
    // 어느 필드의 문제인지 정확히 알 수 없어서 fieldErrors는 {}
    // 응답이 같아서 한 메서드로 묶음 -> 매개변수는 두 예외의 공통 부모인 Exception으로 받음
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnreadableBody(Exception e) {
        // 내부 정보 노출 방지를 위해 원인 메시지는 서버 로그에만 남기고 응답에는 넣지 않음
        log.debug("Unreadable request: {}", e.getMessage());
        return toResponse(ErrorCode.VALIDATION_ERROR);
    }

    // [명세 외 코드] 없는 URL 요청 -> 404 NOT_FOUND
    // 매핑된 컨트롤러가 없으면 정적 리소스 처리로 넘어가고, 거기서도 없으면 이 예외가 남
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.NOT_FOUND);
    }

    // [명세 외 코드] 경로는 있지만 지원하지 않는 HTTP 메서드 ( 예 : GET 전용 경로에 POST )
    // 405 METHOD_NOT_ALLOWED
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // [명세 외 코드] 컨트롤러 단계에서 권한 거부 ( @PreAuthorize 등, AuthorizationDeniedException 포함 )
    // 필터 단계의 거부는 여기서 처리하지 않음, 컨트롤러 안에서 난 거부는 여기서 처리
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return toResponse(ErrorCode.FORBIDDEN);
    }

    // 바로 위에서 처리하지 못한 모든 예외 ( 예상하지 못한 오류 )
    // 500 INTERNAL_SERVER_ERROR, 응답에는 고정 문구만 내보냄 ( 예외 메시지 노출하지 않음 )
    // 예외 객체를 마지막 인자로 넘겨야 스택 트레이스까지 로그에 남음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}
