package com.kdt.nbe13152team07.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

// Redis 캐시 설정 : @Cacheable 결과를 Redis에 JSON으로 저장 (기본 TTL 1시간)
// 키 : cache:{캐시이름}::{키} (예시 cache:categories::SimpleKey [])
// 역직렬화 허용 타입을 우리 패키지, 컬렉션으로 제한 (Redis 값 조작을 통한 역직렬화 공격 방지)
// TTL은 여기서만 관리
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "cache:";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kdt.nbe13152team07.")
                .allowIfSubType("java.util.")
                .allowIfSubType(Long.class)
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .computePrefixWith(cacheName -> KEY_PREFIX + cacheName + "::")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();

    }
}