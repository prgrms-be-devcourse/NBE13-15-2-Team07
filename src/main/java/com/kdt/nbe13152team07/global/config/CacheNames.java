package com.kdt.nbe13152team07.global.config;

// 캐시 이름 상수 (@Cacheable, @CacheEvict에서 문자열 대신 사용 -> 이름 오타로 다른 캐시를 지우는 실수 방지)
// Redis 키 : cache:{이름}::{키} (CacheConfig에서 접두어 추가)
public final class CacheNames {

    public static final String CATEGORIES = "categories";
    public static final String ROOM_OPTIONS = "roomOptions";

    private CacheNames() {}

}
