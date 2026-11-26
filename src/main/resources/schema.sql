CREATE DATABASE IF NOT EXISTS kdt_project2
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE kdt_project2;

DROP TABLE IF EXISTS user_badge;
DROP TABLE IF EXISTS payment_link;
DROP TABLE IF EXISTS challenge_result;
DROP TABLE IF EXISTS expense;
DROP TABLE IF EXISTS participation;
DROP TABLE IF EXISTS room_excluded_category;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS badge;
DROP TABLE IF EXISTS mcc_mapping;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS users;

-- =========================================================
-- 1. 다른 테이블이 참조하는 기준 테이블 (먼저 생성)
-- =========================================================

CREATE TABLE users
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    kakao_id          VARCHAR(30)  COLLATE utf8mb4_bin NULL,       -- 카카오 회원번호. 탈퇴 시 NULL로 지워 재가입 허용
    nickname          VARCHAR(20)  NOT NULL,                       -- 입력 규칙 2~12자, 카카오 닉네임 여유분
    profile_image_url VARCHAR(500) ,
    role              VARCHAR(20)  NOT NULL,                       -- USER / ADMIN
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        DATETIME     NULL,                           -- 탈퇴 시각 (soft delete, NULL이면 활성 회원)
    rejoinable_from   DATE         NULL,                           -- 참여 취소·포기 다음 날. 이 날짜부터 다시 참여 가능 (NULL이면 제한 없음)
    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uk_users_kakao_id UNIQUE (kakao_id),           -- NULL끼리는 중복으로 보지 않아 탈퇴자가 여러 명이어도 OK
    -- 활성 회원은 kakao_id 필수, 탈퇴 회원은 kakao_id가 반드시 비어 있어야 함
    CONSTRAINT ck_users_withdrawal CHECK ((deleted_at IS NULL) = (kakao_id IS NOT NULL))
) ENGINE = InnoDB;

CREATE TABLE category
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30) NOT NULL,                  -- FOOD, CAFE, UTILITY ...
    name       VARCHAR(30) NOT NULL,                  -- 식비, 카페, 공과금 ...
    excludable BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_category      PRIMARY KEY (id),
    CONSTRAINT uk_category_code UNIQUE (code)
) ENGINE = InnoDB;


CREATE TABLE badge
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_badge      PRIMARY KEY (id),
    CONSTRAINT uk_badge_code UNIQUE (code)
) ENGINE = InnoDB;

-- =========================================================
-- 2. 챌린지 방
-- =========================================================

CREATE TABLE room
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    title        VARCHAR(30) NOT NULL,                -- 공백 제거 후 1~30자
    period_type  VARCHAR(20) NOT NULL,                -- WEEK1 / WEEK2 / WEEK4
    budget_limit INT         NOT NULL,                -- 기간별 허용 값은 코드 상수로 검증
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,                -- 서버 계산: start_date + 기간 일수 - 1
    visibility   VARCHAR(20) NOT NULL,                -- PUBLIC / PRIVATE
    invite_code  CHAR(6)     COLLATE utf8mb4_bin,   -- 대문자·숫자 6자리 (입력값은 서버에서 대문자로 변환)
    max_members  INT         NOT NULL,
    status       VARCHAR(20) NOT NULL,                -- RECRUITING / IN_PROGRESS / SETTLING / SETTLED / CANCELED
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_room             PRIMARY KEY (id),
    CONSTRAINT uk_room_invite_code UNIQUE (invite_code),
    CONSTRAINT ck_room_period      CHECK (end_date >= start_date),
    CONSTRAINT ck_room_budget      CHECK (budget_limit > 0),
    CONSTRAINT ck_room_members     CHECK (max_members BETWEEN 2 AND 50),
    CONSTRAINT ck_room_invite_code CHECK (invite_code REGEXP '^[A-Z0-9]{6}$'),   -- bin이라 소문자는 거부됨
    INDEX idx_room_status_start (status, start_date)  -- 상태 전환 스케줄러, 공개 방 목록용
) ENGINE = InnoDB;

CREATE TABLE room_excluded_category
(
    room_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_room_excluded_category PRIMARY KEY (room_id, category_id),
    CONSTRAINT fk_rec_room     FOREIGN KEY (room_id)     REFERENCES room (id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB;

-- =========================================================
-- 3. 참여 / 지출 / 결과
-- =========================================================

CREATE TABLE participation
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    room_id          BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL,            -- ACTIVE / GIVEN_UP / COMPLETED (모집·진행 구분은 room.status로)
    joined_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    given_up_at      DATETIME    NULL,                -- 포기한 경우에만 값이 들어감
    ranking_excluded BOOLEAN     NOT NULL DEFAULT FALSE,   -- 진행 중 통장 해제 시 TRUE → 순위·보상 제외
    -- ACTIVE일 때만 user_id, 그 외엔 NULL → 한 사람당 활성 참여 1개를 DB가 보장
    active_user_id   BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN user_id END) STORED,
    CONSTRAINT pk_participation             PRIMARY KEY (id),
    CONSTRAINT fk_participation_user        FOREIGN KEY (user_id) REFERENCES users (id),   -- RESTRICT: 확정된 기록 보존
    CONSTRAINT fk_participation_room        FOREIGN KEY (room_id) REFERENCES room (id) ON DELETE CASCADE,
    CONSTRAINT uk_participation_user_room   UNIQUE (user_id, room_id),
    CONSTRAINT uk_participation_active_user UNIQUE (active_user_id),
    CONSTRAINT uk_participation_id_room     UNIQUE (id, room_id)   -- CHALLENGE_RESULT의 (participation_id, room_id) 복합 FK 대상
) ENGINE = InnoDB;

CREATE TABLE expense
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    participation_id BIGINT       NOT NULL,
    category_id      BIGINT       NOT NULL,
    amount           INT          NOT NULL,
    merchant_name    VARCHAR(100) NOT NULL,
    spent_at         DATETIME     NOT NULL,           -- 결제 승인 시각(approvedAt)
    payment_tx_id    VARCHAR(64)  COLLATE utf8mb4_0900_bin NOT NULL,   -- 외부 거래 ID: 대소문자·끝 공백까지 정확히 비교 (NO PAD)
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at      DATETIME     NULL,               -- 승인 취소 시각. 삭제 대신 표시해 재전송 시 부활 방지
    CONSTRAINT pk_expense               PRIMARY KEY (id),
    CONSTRAINT fk_expense_participation FOREIGN KEY (participation_id) REFERENCES participation (id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_category      FOREIGN KEY (category_id)      REFERENCES category (id),
    CONSTRAINT uk_expense_payment_tx_id UNIQUE (payment_tx_id),
    CONSTRAINT ck_expense_amount        CHECK (amount BETWEEN 1 AND 10000000),
    INDEX idx_expense_participation_spent (participation_id, spent_at)
) ENGINE = InnoDB;

CREATE TABLE challenge_result
(
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    participation_id BIGINT   NOT NULL,
    room_id          BIGINT   NOT NULL,               -- 랭킹을 방 단위로 인덱스 정렬하기 위한 비정규화 컬럼
    used_amount      INT      NOT NULL DEFAULT 0,
    excluded_amount  INT      NOT NULL DEFAULT 0,
    saved_amount     INT      NOT NULL DEFAULT 0,     -- 음수 가능
    rank_no          INT      NULL,                   -- 순위 대상이 아니면 NULL (순위 대상 여부 = rank_no IS NOT NULL)
    success          BOOLEAN  NOT NULL DEFAULT FALSE,
    settled_at       DATETIME NOT NULL,
    CONSTRAINT pk_challenge_result               PRIMARY KEY (id),
    -- room_id가 실제 참여의 방과 다르게 들어가지 못하도록 (participation_id, room_id)를 묶어서 참조
    CONSTRAINT fk_challenge_result_participation FOREIGN KEY (participation_id, room_id)
        REFERENCES participation (id, room_id) ON DELETE CASCADE,
    -- participation_id 하나에 room_id는 하나뿐이라 UNIQUE(participation_id)와 같은 효과 (정산 중복 방지)
    -- + 위 복합 FK의 인덱스 역할까지 겸함
    CONSTRAINT uk_challenge_result_participation UNIQUE (participation_id, room_id),
    CONSTRAINT ck_challenge_result_rank          CHECK (rank_no IS NULL OR rank_no >= 1),
    -- 방 랭킹: WHERE room_id = ? AND rank_no IS NOT NULL ORDER BY rank_no, id → 정렬 없이 인덱스 순서대로 읽음
    -- (InnoDB 보조 인덱스 끝에는 PK(id)가 자동으로 붙어 동점자 정렬·페이징도 안정적)
    INDEX idx_challenge_result_room_rank (room_id, rank_no)
) ENGINE = InnoDB;

-- =========================================================
-- 4. 결제 연동 / 칭호
-- =========================================================

CREATE TABLE payment_link
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    fintech_use_num CHAR(24)    NOT NULL,             -- 숫자 24자리 고정
    status          VARCHAR(20) NOT NULL,             -- ACTIVE / REVOKED
    linked_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payment_link         PRIMARY KEY (id),
    CONSTRAINT fk_payment_link_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_link_user    UNIQUE (user_id),
    CONSTRAINT uk_payment_link_fintech UNIQUE (fintech_use_num)
) ENGINE = InnoDB;

CREATE TABLE user_badge
(
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    user_id           BIGINT   NOT NULL,
    badge_id          BIGINT   NOT NULL,
    is_representative BOOLEAN  NOT NULL DEFAULT FALSE,
    earned_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 대표 칭호일 때만 user_id, 그 외엔 NULL → 한 사람당 대표 칭호 1개를 DB가 보장
    rep_user_id       BIGINT GENERATED ALWAYS AS
        (CASE WHEN is_representative THEN user_id END) STORED,
    CONSTRAINT pk_user_badge                PRIMARY KEY (id),
    CONSTRAINT fk_user_badge_user           FOREIGN KEY (user_id)  REFERENCES users (id),  -- 생성 컬럼의 기준 컬럼이라 CASCADE 불가
    CONSTRAINT fk_user_badge_badge          FOREIGN KEY (badge_id) REFERENCES badge (id),
    CONSTRAINT uk_user_badge_user_badge     UNIQUE (user_id, badge_id),
    CONSTRAINT uk_user_badge_representative UNIQUE (rep_user_id)
) ENGINE = InnoDB;
