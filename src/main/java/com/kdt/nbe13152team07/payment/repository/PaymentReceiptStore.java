package com.kdt.nbe13152team07.payment.repository;

import com.kdt.nbe13152team07.payment.dto.PaymentReceiptResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

// 모의 결제 서버의 재시도로 같은 결제가 두 번 처리되지 않게 막는 Redis 저장소 (X1 승인, X2 취소)
@Component
public class PaymentReceiptStore {

    // 승인(X1)과 취소(X2)는 같은 transactionId로 들어온다.
    // 키를 하나만 쓰면 승인 결과가 남아 있는 10분 동안 취소가 "이미 처리됨"으로 막히므로 키 앞부분을 나눈다.
    public enum Type {
        APPROVE("payment:tx:"),       // X1 승인 (기존 키 그대로라 이미 저장된 데이터와 호환)
        CANCEL("payment:cancel:");    // X2 취소

        private final String prefix;

        Type(String prefix) {
            this.prefix = prefix;
        }
    }

    private static final String PROCESSING = "PROCESSING";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PaymentReceiptStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    // 처음 들어온 결제면 "처리 중" 깃발을 꽂고 true, 이미 처리 중이거나 처리된 결제면 false (재시도 중복 방지)
    public boolean tryStart(Type type, String transactionId) {
        // 키가 없을 때만 "PROCESSING"을 10분짜리로 저장 (Redis: SET 키 값 NX EX 600)
        Boolean started = redisTemplate.opsForValue()
                .setIfAbsent(key(type, transactionId), PROCESSING, TTL);
        // null이 와도 false가 되도록 Boolean.TRUE.equals로 비교
        return Boolean.TRUE.equals(started);
    }

    // 처리 결과를 저장 (PROCESSING 깃발을 결과로 덮어씀)
    public void saveResult(Type type, String transactionId, PaymentReceiptResponse receipt) {
        String json = objectMapper.writeValueAsString(receipt);
        redisTemplate.opsForValue()
                .set(key(type, transactionId), json, TTL);
    }

    // 저장된 결과 꺼내기. 키가 없거나 아직 처리 중이면 비어 있음
    public Optional<PaymentReceiptResponse> findResult(Type type, String transactionId) {
        String result = redisTemplate.opsForValue().get(key(type, transactionId));
        if (result == null || result.equals(PROCESSING)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(result, PaymentReceiptResponse.class));
    }

    //명세에 없는 부분 추가(처리 도중 실패하면 깃발을 지워서 재시도가 다시 처리될 수 있게)
    public void release(Type type, String transactionId) {
        redisTemplate.delete(key(type, transactionId));
    }

    // 예) APPROVE + tx_001 → "payment:tx:tx_001", CANCEL + tx_001 → "payment:cancel:tx_001"
    private String key(Type type, String transactionId) {
        return type.prefix + transactionId;
    }

}
