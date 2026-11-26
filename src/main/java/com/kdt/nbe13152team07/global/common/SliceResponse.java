package com.kdt.nbe13152team07.global.common;

import org.springframework.data.domain.Slice;

import java.util.List;

// 다음 페이지 유무만 알려주는 목록 공통 응답
public record SliceResponse<T>(
    List<T> content,
    int page,
    int size,
    boolean hasNext
) {

    // Spring Slice -> SliceResponse 변환
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<T>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }

}
