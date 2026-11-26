package com.kdt.nbe13152team07.room;

import java.security.SecureRandom;

/**
 * 비공개 방의 초대 코드 생성기.
 * - 영문 대문자 + 숫자로 된 8자리 코드 (예: "K7M2QX9A")
 * - 공개 방(PUBLIC)은 초대 코드를 쓰지 않으므로 null로 두고, 비공개 방(PRIVATE)만 R2에서 발급한다.
 * - 초대 코드는 방을 만들 때 한 번 정해지고 바뀌지 않는다 (v4.1에서 R10 초대 코드 재발급 삭제).
 * - R6 방 참여에서 이 코드로 방을 찾으며, 없는 코드면 404 INVALID_INVITE_CODE.
 * 근거: API 명세서 v4.1 R2·R4·R6, 0장 변경 내역("8자리 코드, 공개 방은 null")
 * 참고: 예측하기 어려운 코드여야 하므로 Random 대신 SecureRandom을 사용한다.
 *       드물게 기존 코드와 겹칠 수 있으므로, 저장 전 중복 확인(또는 DB 유니크 제약)이 필요하다.
 */
public class InviteCodeGenerator {

    // 코드에 쓸 문자 집합: A~Z, 0~9 (36개)
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    // 코드 길이: 8자리 (room.invite_code 컬럼 길이와 같음)
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    /**
     * 8자리 초대 코드를 새로 만든다.
     * @return 영문 대문자·숫자 8자리 문자열
     */
    public String generate() {

        StringBuilder stringBuilder = new StringBuilder();
        // 문자 집합에서 무작위로 한 글자씩 골라 LENGTH만큼 이어 붙인다.
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            char ch = CHARACTERS.charAt(index);
            stringBuilder.append(ch);
        }
        return  stringBuilder.toString();
    }
}
