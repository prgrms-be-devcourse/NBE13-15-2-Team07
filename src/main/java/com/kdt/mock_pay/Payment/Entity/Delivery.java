package com.kdt.mock_pay.Payment.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DeliveryEventType eventType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DeliveryTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    private Integer httpCode;     // 타임아웃·연결 실패면 null

    @Column(length = 50)
    private String result;        // 우리 서버 응답의 result 또는 code

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private FailureReason errorType;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    private Delivery(Payment payment, DeliveryEventType eventType, DeliveryTrigger triggerType,
                     DeliveryStatus status, Integer httpCode, String result, FailureReason errorType,
                     LocalDateTime requestedAt, LocalDateTime respondedAt) {
        this.payment = payment;
        this.eventType = eventType;
        this.triggerType = triggerType;
        this.status = status;
        this.httpCode = httpCode;
        this.result = result;
        this.errorType = errorType;
        this.requestedAt = requestedAt;
        this.respondedAt = respondedAt;
    }

    // 2xx 응답
    public static Delivery success(Payment payment, DeliveryEventType eventType, DeliveryTrigger triggerType,
                                   int httpCode, String result,
                                   LocalDateTime requestedAt, LocalDateTime respondedAt) {
        return new Delivery(payment, eventType, triggerType, DeliveryStatus.SUCCESS,
                httpCode, result, null, requestedAt, respondedAt);
    }

    // 4xx·5xx 응답(httpCode 있음) 또는 타임아웃(httpCode·respondedAt 없음)
    public static Delivery failure(Payment payment, DeliveryEventType eventType, DeliveryTrigger triggerType,
                                   FailureReason errorType, Integer httpCode, String result,
                                   LocalDateTime requestedAt, LocalDateTime respondedAt) {
        return new Delivery(payment, eventType, triggerType, DeliveryStatus.FAILED,
                httpCode, result, errorType, requestedAt, respondedAt);
    }
}
