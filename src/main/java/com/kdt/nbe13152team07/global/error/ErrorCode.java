package com.kdt.nbe13152team07.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 핸들러가 상태와 메시지를 읽을 수 있도록 -> getStatus(), getMessage()
@Getter
// final 필드를 받는 생성자 자동 생성
@RequiredArgsConstructor
// 왜 enum인가? -> 오류 종류를 한곳에 모으고, 정해진 값만 쓰게 해서 오타나 중복을 막기 위해
public enum ErrorCode {

    // 400 Error 처리

    // 요청 값 검증 실패 : 필수값 누락, 형식 및 범위 오류(닉네임 길이, 상한선, 시작일 등), JSON 문법 오류, 파라미터 타입 오류 등
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),

    // --------------------------------------------------------------------------------------------------------------

    // 401 Error 처리

    // 액세스 토큰이 없거나 위조 및 형식 오류, refresh 토큰이 유효하지 않음
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    // 액세스 토큰 만료됨
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    // 모의 결제 서버가 X1, X2를 호출할 때 X-API-KEY가 틀림
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "결제 서버 인증에 실패했습니다."),
    // 탈퇴한 회원이 로그인하거나 토큰을 재발급할 때
    USER_WITHDRAWN(HttpStatus.UNAUTHORIZED, "탈퇴한 회원입니다."),

    // --------------------------------------------------------------------------------------------------------------

    // 403 Error 처리

    // [명세 외] 역할 권한 부족 (일반 회원의 admin/** 호출)
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    // 다른 사람의 참여, 지출, 결과에 접근
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 기록만 볼 수 있습니다."),
    // 방 참여자가 아닌데 참여자 목록, 랭킹, 비공개 방 상세 조회
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "방 참여자만 볼 수 있습니다."),
    // 비공개 방에 roomId로 참여하려 할 때
    INVITE_CODE_REQUIRED(HttpStatus.FORBIDDEN, "비공개 방은 초대 코드로 참여해 주세요."),

    // --------------------------------------------------------------------------------------------------------------

    // 404 Error 처리

    // [명세 외] 존재하지 않는 URL 요청
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    // 회원이 없음. 탈퇴 회원의 로그인·재발급에는 USER_WITHDRAWN을 씀
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    // 방이 없음(없는 roomId)
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    // 없는 참여 id
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    // 연결된 통장이 없는데 조회 및 해제
    PAYMENT_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    // 해당 초대 코드의 방이 없음
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "초대 코드가 올바르지 않습니다."),

    // --------------------------------------------------------------------------------------------------------------

    // 405 Error 처리

    // [명세 외] 경로는 있지만 지원하지 않는 HTTP 메서드 (예 : GET 전용 경로에 POST)
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),

    // --------------------------------------------------------------------------------------------------------------

    // 409 Error 처리

    // 모집 중이거나 진행 중인 방에 이미 참여되어 있는데 방 생성 및 참여
    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "이미 참여 중인 방이 있습니다."),
    // 참여 취소·포기한 당일에 다른 방 생성 및 참여
    REJOIN_NOT_ALLOWED_YET(HttpStatus.CONFLICT, "참여 취소·포기한 다음 날부터 참여할 수 있습니다."),
    // 결제 통장 연결 없이 방 생성 및 참여
    PAYMENT_LINK_REQUIRED(HttpStatus.CONFLICT, "결제 통장을 먼저 연결해 주세요."),
    // 이미 연결된 통장이 있는데 다시 연결
    ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연결된 통장이 있습니다."),
    // 모집이 끝난(이미 시작한) 방에 참여 및 참여 취소
    ROOM_NOT_RECRUITING(HttpStatus.CONFLICT, "모집이 끝난 방입니다."),
    // 정원이 다 찬 방에 참여
    ROOM_FULL(HttpStatus.CONFLICT, "정원이 찼습니다."),
    // 시작 전 방에서 포기 요청 -> 참여 취소를 이용해야 함
    ROOM_NOT_STARTED(HttpStatus.CONFLICT, "시작 전에는 포기할 수 없습니다. 참여 취소를 이용해 주세요."),
    // 정산 전에 결과 및 랭킹 조회
    NOT_SETTLED_YET(HttpStatus.CONFLICT, "아직 정산 전입니다."),
    // 다른 회원이 이미 연결한 통장으로 연결할 때
    FINTECH_NUM_IN_USE(HttpStatus.CONFLICT, "다른 사용자가 연결한 통장입니다."),
    // 진행 중이 아닌 참여를 포기하려 할 때
    CHALLENGE_NOT_ACTIVE(HttpStatus.CONFLICT, "진행 중인 챌린지가 아닙니다."),
    // 종료일이 지난 뒤 결제 취소가 들어올 때
    CHALLENGE_ENDED(HttpStatus.CONFLICT, "챌린지가 종료되었습니다."),
    // Redis 락을 못 잡았을 때
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "요청을 처리하고 있습니다. 잠시 후 다시 시도해 주세요."),
    // 인원이 모자라 자동 삭제(DELETED)된 방에 요청할 때
    ROOM_CANCELED(HttpStatus.CONFLICT, "인원이 모자라 취소된 방입니다."),
    // [제안] -> NICKNAME_DUPLICATED 추가 여부

    // --------------------------------------------------------------------------------------------------------------

    // 500 Error 처리

    // [명세 외] 처리하지 못한 예외
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    ;   // 상수 정의 끝

    private final HttpStatus status;    // 상수마다 HTTP 상태를 정해서 서비스가 상태 코드를 따로 신경 쓰지 않게 함
    private final String message;       // 같은 오류에는 항상 같은 문구가 나갈 수 있도록

    // code 필드가 없는 이유 -> 응답의 code는 상수 이름과 같아서 name()으로 꺼내 쓰면 되기 때문

}
