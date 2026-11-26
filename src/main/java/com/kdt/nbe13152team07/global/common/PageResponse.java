package com.kdt.nbe13152team07.global.common;

import org.springframework.data.domain.Page;

import java.util.List;

// 페이지 목록 공통 응답 (Page를 그대로 내보내지 않고 명세의 5개 필드만 반환)
// T : content에 담을 항목 타입 (예: 참여 기록 DTO, 방 목록 DTO)
// 왜 제네릭을 쓰는가?
//      -> PageResponse는 여러 API가 함께 쓰는 툴인데, API마다 content에 들어가는 항목이 다름
//      -> 이거 안 쓰면 API마다 따로 만들어야함
//      -> Object를 써도 되지만 타입 정보가 사라짐 (꺼내쓸 때 형변환을 해야함)
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    // Spring Page -> PageResponse 변환
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<T>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

}
