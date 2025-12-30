package com.paymenttech.service;


import com.paymenttech.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_PREFIX = "payment:";
    private static final long CACHE_TTL_MINUTES = 30;
    
    public void cachePayment(String idempotencyKey, PaymentResponse response) {
        String key = CACHE_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }
    
    public PaymentResponse getCachedPayment(String idempotencyKey) {
        String key = CACHE_PREFIX + idempotencyKey;
        return (PaymentResponse) redisTemplate.opsForValue().get(key);
    }
    
    public void invalidateCache(String idempotencyKey) {
        String key = CACHE_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
    }
    
    public void incrementFailureCount(String sourceAccount) {
        String key = "failures:" + sourceAccount;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }
    
    public long getFailureCount(String sourceAccount) {
        String key = "failures:" + sourceAccount;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Long) count : 0;
    }
}