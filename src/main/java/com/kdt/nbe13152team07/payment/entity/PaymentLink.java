package com.kdt.nbe13152team07.payment.entity;

import com.kdt.nbe13152team07.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_link", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_link_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_payment_link_fintech", columnNames = "fintech_use_num")
})
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fintech_use_num", nullable = false, columnDefinition = "CHAR(24)")
    private String fintechUseNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentLinkStatus status;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    protected PaymentLink() {
    }

    private PaymentLink(User user, String fintechUseNum, LocalDateTime now) {
        this.user = user;
        this.fintechUseNum = fintechUseNum;
        this.status = PaymentLinkStatus.ACTIVE;
        this.linkedAt = now;
    }

    public static PaymentLink link(User user, String fintechUseNum, LocalDateTime now) {
        return new PaymentLink(user, fintechUseNum, now);
    }

    /** 해제했던 사용자가 다시 연결할 때. uk_payment_link_user 때문에 새 행 대신 기존 행을 되살린다. */
    public void relink(String fintechUseNum, LocalDateTime now) {
        if (isActive()) {
            throw new IllegalStateException("이미 연결된 계좌입니다.");
        }
        this.fintechUseNum = fintechUseNum;
        this.status = PaymentLinkStatus.ACTIVE;
        this.linkedAt = now;
    }

    public void revoke() {
        if (!isActive()) {
            throw new IllegalStateException("이미 해제된 계좌입니다.");
        }
        this.status = PaymentLinkStatus.REVOKED;
    }

    public boolean isActive() {
        return status == PaymentLinkStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFintechUseNum() { return fintechUseNum; }
    public PaymentLinkStatus getStatus() { return status; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
}