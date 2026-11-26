package com.kdt.nbe13152team07.payment.repository;

import com.kdt.nbe13152team07.payment.dto.PaymentReceiptResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Component
public class PaymentReceiptStore {

    private static final String KEY_PREFIX = "payment:tx:";
    private static final String PROCESSING = "PROCESSING";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PaymentReceiptStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    // 처음 들어온 결제면 "처리 중" 깃발을 꽂고 true, 이미 처리 중이거나 처리된 결제면 false (재시도 중복 방지)
    public boolean tryStart(String transactionId) {
        // 키가 없을 때만 "PROCESSING"을 10분짜리로 저장 (Redis: SET 키 값 NX EX 600)
        Boolean started = redisTemplate.opsForValue()
                .setIfAbsent(key(transactionId), PROCESSING, TTL);
        // null이 와도 false가 되도록 Boolean.TRUE.equals로 비교
        return Boolean.TRUE.equals(started);
    }

    // 처리 결과를 저장 (PROCESSING 깃발을 결과로 덮어씀)
    public void saveResult(String transactionId, PaymentReceiptResponse receipt) {
        String json = objectMapper.writeValueAsString(receipt);
        redisTemplate.opsForValue()
                .set(key(transactionId), json, TTL);
    }

    // 저장된 결과 꺼내기. 아직 처리 중이면 비어 있음
    public Optional<PaymentReceiptResponse> findResult(String transactionId) {
        String result = redisTemplate.opsForValue().get(key(transactionId));
        if (result == null){
            return Optional.empty();
        }
        else if (result.equals(PROCESSING)){
            return Optional.empty();
        }
        else{
            return Optional.of(objectMapper.readValue(result, PaymentReceiptResponse.class));
        }
    }

    private String key(String transactionId) {
        return KEY_PREFIX + transactionId;
    }

    //명세에 없는 부분 추가(처리 도중 실패하면 깃발을 지워서 재시도가 다시 처리될 수 있게)
    public void release(String transactionId) {
        redisTemplate.delete(key(transactionId));
    }

}