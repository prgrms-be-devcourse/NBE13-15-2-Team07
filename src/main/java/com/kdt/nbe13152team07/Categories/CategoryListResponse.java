package com.kdt.nbe13152team07.Categories;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.List;

// 새로 추가: 전체 목록 캐싱용
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
public record CategoryListResponse(
        List<CategoryResponse> content
) implements Serializable {
}