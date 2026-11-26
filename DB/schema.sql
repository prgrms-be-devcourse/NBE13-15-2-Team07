-- 모의 결제 서버 전용 DB (우리 서버 DB와 분리)
-- Docker: docker-compose.yml이 컨테이너를 처음 만들 때 자동 실행 (직접 실행해도 됨)
CREATE DATABASE IF NOT EXISTS mock_pay
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE mock_pay;

-- 재실행용: 참조하는 쪽(자식)부터 삭제. 개발·테스트 환경에서만 실행
DROP TABLE IF EXISTS mock_delivery;
DROP TABLE IF EXISTS mock_ledger;
DROP TABLE IF EXISTS mock_payment;
DROP TABLE IF EXISTS mock_account;

-- =========================================================
-- 1. 가짜 통장 (M1)
-- =========================================================
CREATE TABLE mock_account
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    fintech_use_num CHAR(24)    NOT NULL,             -- 우리 서버와 공유하는 키
    owner_name      VARCHAR(30) NOT NULL,
    bank_name       VARCHAR(30) NOT NULL,             -- 화면 표시용 (예: 모의은행)
    balance         BIGINT      NOT NULL DEFAULT 0,   -- 승인 성공 시 차감, 취소 성공 시 복구
    status          VARCHAR(20) NOT NULL,             -- ACTIVE / CLOSED
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mock_account         PRIMARY KEY (id),
    CONSTRAINT uk_mock_account_fintech UNIQUE (fintech_use_num),
    CONSTRAINT ck_mock_account_fintech CHECK (fintech_use_num REGEXP '^[0-9]{24}$'),
    CONSTRAINT ck_mock_account_balance CHECK (balance >= 0)
) ENGINE = InnoDB;

-- =========================================================
-- 2. 결제 (M2 생성 → M3 승인 / M4 거절 / M5 취소, M6 내역)
-- =========================================================
CREATE TABLE mock_payment
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    transaction_id         VARCHAR(64)  COLLATE utf8mb4_0900_bin NOT NULL,
    account_id             BIGINT       NOT NULL,
    amount                 INT          NOT NULL,
    merchant_name          VARCHAR(100) NOT NULL,
    merchant_category_code CHAR(4)      NOT NULL,
    paid_at                DATETIME     NOT NULL,     -- 요청의 paidAt. X1에 paidAt으로 그대로 전달
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED / FAILED / CANCELED
    reject_reason          VARCHAR(30)  NULL,         -- INSUFFICIENT_BALANCE(잔액 부족 자동 거절) / MANUAL(M4)
    failure_reason         VARCHAR(30)  NULL,         -- MERCHANT_TIMEOUT / MERCHANT_REJECTED / MERCHANT_ERROR
    approved_at            DATETIME     NULL,
    rejected_at            DATETIME     NULL,
    canceled_at            DATETIME     NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_mock_payment         PRIMARY KEY (id),
    CONSTRAINT fk_mock_payment_account FOREIGN KEY (account_id) REFERENCES mock_account (id),
    CONSTRAINT uk_mock_payment_tx      UNIQUE (transaction_id),
    CONSTRAINT ck_mock_payment_amount  CHECK (amount BETWEEN 1 AND 10000000),
    CONSTRAINT ck_mock_payment_mcc     CHECK (merchant_category_code REGEXP '^[0-9]{4}$'),
    -- 상태별로 있어야 할 시각·사유만 채워져 있어야 함
    CONSTRAINT ck_mock_payment_state CHECK (
        (status = 'PENDING'  AND approved_at IS NULL     AND rejected_at IS NULL     AND canceled_at IS NULL     AND reject_reason IS NULL     AND failure_reason IS NULL) OR
        (status = 'APPROVED' AND approved_at IS NOT NULL AND rejected_at IS NULL     AND canceled_at IS NULL     AND reject_reason IS NULL     AND failure_reason IS NULL) OR
        (status = 'REJECTED' AND approved_at IS NULL     AND rejected_at IS NOT NULL AND canceled_at IS NULL     AND reject_reason IS NOT NULL AND failure_reason IS NULL) OR
        (status = 'FAILED'   AND approved_at IS NULL     AND rejected_at IS NULL     AND canceled_at IS NULL     AND reject_reason IS NULL     AND failure_reason IS NOT NULL) OR
        (status = 'CANCELED' AND approved_at IS NOT NULL AND rejected_at IS NULL     AND canceled_at IS NOT NULL AND reject_reason IS NULL     AND failure_reason IS NULL)
    ),
    INDEX idx_mock_payment_account_paid (account_id, paid_at)   -- M6: from~to 범위, 결제 시각 내림차순
) ENGINE = InnoDB;

-- =========================================================
-- 3. 통장 입출금 내역 (잔액이 바뀔 때마다 한 줄)
-- =========================================================
CREATE TABLE mock_ledger
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    account_id    BIGINT      NOT NULL,
    payment_id    BIGINT      NULL,                   -- 입금(DEPOSIT)은 결제가 없으므로 NULL
    entry_type    VARCHAR(20) NOT NULL,               -- DEPOSIT / PAYMENT(승인 출금) / REFUND(취소 환불)
    amount        BIGINT      NOT NULL,               -- 입금·환불은 +, 출금은 -
    balance_after BIGINT      NOT NULL,               -- 이 거래 직후 잔액
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mock_ledger         PRIMARY KEY (id),
    CONSTRAINT fk_mock_ledger_account FOREIGN KEY (account_id) REFERENCES mock_account (id),
    CONSTRAINT fk_mock_ledger_payment FOREIGN KEY (payment_id) REFERENCES mock_payment (id),
    CONSTRAINT uk_mock_ledger_payment UNIQUE (payment_id, entry_type),   -- 같은 결제를 두 번 출금·환불 방지
    CONSTRAINT ck_mock_ledger_sign CHECK (
        (entry_type IN ('DEPOSIT', 'REFUND') AND amount > 0) OR
        (entry_type = 'PAYMENT' AND amount < 0)
        ),
    CONSTRAINT ck_mock_ledger_payment CHECK ((entry_type = 'DEPOSIT') = (payment_id IS NULL)),
    INDEX idx_mock_ledger_account_id (account_id, id)    -- 입출금 내역 페이징 (최신순)
) ENGINE = InnoDB;

-- =========================================================
-- 4. 우리 서버 호출 기록 (X1·X2 한 번 호출할 때마다 한 줄, 재시도 없음)
-- =========================================================
CREATE TABLE mock_delivery
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    payment_id     BIGINT      NOT NULL,
    event_type     VARCHAR(20) NOT NULL,              -- APPROVE(X1) / CANCEL(X2)
    trigger_type   VARCHAR(20) NOT NULL,              -- USER(M3·M5) / NET_CANCEL(타임아웃 후 망취소)
    status         VARCHAR(20) NOT NULL,              -- SUCCESS(2xx) / FAILED
    http_code      INT         NULL,                  -- 타임아웃·연결 실패면 NULL
    result         VARCHAR(50) NULL,                  -- 우리 서버 응답 result 또는 code (RECORDED, DUPLICATE, CHALLENGE_ENDED ...)
    error_type     VARCHAR(30) NULL,                  -- 실패 시 MERCHANT_TIMEOUT / MERCHANT_REJECTED / MERCHANT_ERROR
    requested_at   DATETIME    NOT NULL,
    responded_at   DATETIME    NULL,
    CONSTRAINT pk_mock_delivery         PRIMARY KEY (id),
    CONSTRAINT fk_mock_delivery_payment FOREIGN KEY (payment_id) REFERENCES mock_payment (id),
    CONSTRAINT ck_mock_delivery_result  CHECK ((status = 'SUCCESS') = (error_type IS NULL)),
    INDEX idx_mock_delivery_payment (payment_id, id)     -- 결제별 호출 이력
) ENGINE = InnoDB;
