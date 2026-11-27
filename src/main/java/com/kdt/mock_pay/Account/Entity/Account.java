package com.kdt.mock_pay.Account.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 24, unique = true)
    private String fintechUseNum;

    @Column(nullable = false, length = 30)
    private String ownerName;

    @Column(nullable = false, length = 30)
    private String bankName;

    @Column(nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Account(String fintechUseNum, String ownerName, String bankName, LocalDateTime now){
        this.fintechUseNum = fintechUseNum;
        this.ownerName = ownerName;
        this.bankName = bankName;
        this.balance = 0;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = now;
    }

    public static Account open(String fintechUseNum, String ownerName, String bankName, LocalDateTime now){
        return new Account(fintechUseNum, ownerName, bankName, now);
    }

    public boolean canPay(long amount){
        return balance >= amount;
    }

    public long deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금액이 0보다 커야합니다.");
        }
        if (balance < amount){
            throw new IllegalStateException("잔액 부족");
        }
        balance -= amount;
        return balance;
    }
}
