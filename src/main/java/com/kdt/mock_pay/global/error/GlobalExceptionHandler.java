package com.kdt.mock_pay.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

@RestControllerAdvice //모든 컨트롤러에서 난 예외를 여기서 잡음
@Slf4j //log 필드 자동 생성
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode){
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, Map<String, String> fieldErrors){
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, fieldErrors));
    }

    //서비스 예외와 400 검증 오류

    //서비스 규칙 위반 -> ErrorCode가 정한 상태와 문구
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e){
        log.debug("BusinessException: {}", e.getMessage());
        return toResponse(e.getErrorCode());
    }

    // @Valid, @RequestBody 검증 실패 -> fieldErrors = {필드 이름 : 어노테이션 message}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e){
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for(FieldError error : e.getBindingResult().getFieldErrors()){
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return toResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    //@PathVariable, @RequestParam 검증 실패 (ex : 핀테크이용번호가 다름)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e){
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for(ParameterValidationResult result : e.getParameterValidationResults()){
            if(result instanceof ParameterErrors errors){
                for(FieldError error : errors.getFieldErrors()){
                    fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
                }
                continue;
            }

            String name = result.getMethodParameter().getParameterName();
            String message = result.getResolvableErrors().get(0).getDefaultMessage();
            fieldErrors.putIfAbsent(name, message);
        }

        return toResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    // 파라미터 타입 변환 실패 (예 : ?from=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return toResponse(ErrorCode.VALIDATION_ERROR, Map.of(e.getName(), "형식이 올바르지 않습니다."));
    }

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return toResponse(ErrorCode.VALIDATION_ERROR, Map.of(e.getParameterName(), "필수 값입니다."));
    }

    // JSON 문법 오류, 본문 필드 타입 오류, JSON이 아닌 본문 → 어느 필드인지 몰라서 fieldErrors {}
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnreadableBody(Exception e) {
        log.debug("Unreadable request: {}", e.getMessage());
        return toResponse(ErrorCode.VALIDATION_ERROR);
    }

    //[명세 외] 없는 URL -> 404 NOT_FOUND
    @ExceptionHandler(NoResourceFoundException.class)
    public  ResponseEntity<ErrorResponse> handleNoResource (NoResourceFoundException e){
        return toResponse(ErrorCode.NOT_FOUND);
    }

    //[명세 외] 지원하지 않는 HTTP 메서드 -> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpMediaTypeNotSupportedException e){
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    //[명세 외] 위에서 못 잡은 모든 예외 -> 500 응답엔 고정 문구만
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e){
        log.error("Unhandled exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}


