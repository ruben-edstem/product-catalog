package com.edstem.product_catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenericCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheObject(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void cacheObject(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCachedObject(String key, Class<T> expectedType) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (expectedType.isInstance(cached)) {
            return (T) cached;
        }
        return null;
    }

    public void evictCache(String key) {
        try {
            redisTemplate.delete(key);
            log.info("Evicted cache: {}", key);
        } catch (Exception e) {
            log.info("Error evicting cache with key {}: {}", key, e.getMessage());
        }
    }
}