package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Participation.Participation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name="expense")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="participation_id",nullable=false)
    private Participation participationId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="category_id",nullable=false)
    private Categories categoryId;

    @Column(name="amount",nullable=false)
    private int amount;

    @Column(name="merchant_name",nullable=false)
    private String merchantName;

    @Column(name="spent_at",nullable=false,updatable=false)
    private OffsetDateTime spentAt;

    @Column(name="payment_tx_id",nullable=false)
    private String paymentTxId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name="canceled_at")
    private OffsetDateTime canceledAt;

    public Expense( Participation participationId, Categories categoryId, int amount, String merchantName, OffsetDateTime spentAt, String paymentTxId) {
        this.participationId = participationId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.merchantName = merchantName;
        this.spentAt = spentAt;
        this.paymentTxId = paymentTxId;
        this.canceledAt=null;
    }
}
