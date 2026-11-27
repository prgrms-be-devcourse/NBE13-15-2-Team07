package com.kdt.mock_pay.Account.Entity;

import com.kdt.mock_pay.Payment.Entity.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 입금(DEPOSIT)은 결제가 없으므로 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Column(nullable = false)
    private long amount;          // 입금·환불은 +, 출금은 -

    @Column(nullable = false)
    private long balanceAfter;    // 이 거래 직후 잔액

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Ledger(Account account, Payment payment, LedgerEntryType entryType,
                   long amount, long balanceAfter, LocalDateTime now) {
        this.account = account;
        this.payment = payment;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = now;
    }

    public static Ledger deposit(Account account, long amount, long balanceAfter, LocalDateTime now) {
        return new Ledger(account, null, LedgerEntryType.DEPOSIT, amount, balanceAfter, now);
    }

    // 출금은 음수로 기록
    public static Ledger payment(Payment payment, long balanceAfter, LocalDateTime now) {
        return new Ledger(payment.getAccount(), payment, LedgerEntryType.PAYMENT,
                -payment.getAmount(), balanceAfter, now);
    }

    public static Ledger refund(Payment payment, long balanceAfter, LocalDateTime now) {
        return new Ledger(payment.getAccount(), payment, LedgerEntryType.REFUND,
                payment.getAmount(), balanceAfter, now);
    }
}
