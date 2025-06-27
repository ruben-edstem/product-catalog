package com.edstem.product_catalog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GenericCacheService cacheService;

    private final String testKey = "test:key";
    private final String testValue = "test-value";

    @Test
    void cacheObject_WithoutTimeout_ShouldCallRedisSet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.cacheObject(testKey, testValue);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(testKey, testValue);
    }

    @Test
    void cacheObject_WithTimeout_ShouldCallRedisSetWithTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration duration = Duration.ofMinutes(5);

        cacheService.cacheObject(testKey, testValue, duration);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(testKey, testValue, duration);
    }

    @Test
    void getCachedObject_WhenTypeMatches_ShouldReturnValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(testKey)).thenReturn(testValue);

        String result = cacheService.getCachedObject(testKey, String.class);

        assertEquals(testValue, result);
    }

    @Test
    void getCachedObject_WhenTypeDoesNotMatch_ShouldReturnNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(testKey)).thenReturn(123); // Wrong type

        String result = cacheService.getCachedObject(testKey, String.class);

        assertNull(result);
    }

    @Test
    void getCachedObject_WhenValueIsNull_ShouldReturnNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(testKey)).thenReturn(null);

        String result = cacheService.getCachedObject(testKey, String.class);

        assertNull(result);
    }

    @Test
    void evictCache_WhenSuccessful_ShouldCallDelete() {
        when(redisTemplate.delete(testKey)).thenReturn(true);

        cacheService.evictCache(testKey);

        verify(redisTemplate).delete(testKey);
    }

    @Test
    void evictCache_WhenRedisThrowsException_ShouldNotFail() {
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete(testKey);

        assertDoesNotThrow(() -> cacheService.evictCache(testKey));
        verify(redisTemplate).delete(testKey);
    }
}