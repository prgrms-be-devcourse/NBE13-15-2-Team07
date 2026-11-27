package com.kdt.mock_pay.Payment.Entity;

import com.kdt.mock_pay.Account.Entity.Account;
import com.kdt.mock_pay.global.error.BusinessException;
import com.kdt.mock_pay.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 100)
    private String merchantName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 4)
    private String merchantCategoryCode;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private RejectReason rejectReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private FailureReason failureReason;

    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime canceledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Payment(Account account, String transactionId, int amount, String merchantName,
                    String merchantCategoryCode, LocalDateTime paidAt, LocalDateTime now) {
        this.account = account;
        this.transactionId = transactionId;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
        this.paidAt = paidAt;
        this.status = PaymentStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // M2 : 결제는 항상 PENDING으로 시작
    public static Payment create(Account account, String transactionId, int amount, String merchantName,
                                 String merchantCategoryCode, LocalDateTime paidAt, LocalDateTime now) {
        return new Payment(account, transactionId, amount, merchantName, merchantCategoryCode, paidAt, now);
    }

    // M3 : 우리 서버(X1)가 2xx를 줬을 때
    public void approve(LocalDateTime now) {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = now;
        this.updatedAt = now;
    }

    // M3 : X1이 실패(타임아웃·4xx·5xx)했을 때, 재시도 없음
    public void fail(FailureReason reason, LocalDateTime now) {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    // M4 직접 거절, 또는 M3에서 잔액 부족
    public void reject(RejectReason reason, LocalDateTime now) {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.REJECTED;
        this.rejectReason = reason;
        this.rejectedAt = now;
        this.updatedAt = now;
    }

    // M5 : 우리 서버(X2)가 2xx를 줬을 때만
    public void cancel(LocalDateTime now) {
        requireStatus(PaymentStatus.APPROVED);
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = now;
        this.updatedAt = now;
    }

    // 지금 상태가 기대한 상태가 아니면 409 INVALID_PAYMENT_STATUS
    private void requireStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }
}
