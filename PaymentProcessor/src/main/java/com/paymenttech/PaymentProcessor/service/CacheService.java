package com.paymenttech.PaymentProcessor.service;


import com.paymenttech.PaymentProcessor.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.paymenttech.PaymentProcessor.dto.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate ;
    private static final String CACHE_PREFIX = "payment:";
    private static final long CACHE_TTL_MINUTES = 30;
    
    public void cachePayment(String idempotencyKey, PaymentResponse response) {
        String key = CACHE_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }
    
    public PaymentResponse getCachedPayment(String idempotencyKey) {
        String key = CACHE_PREFIX + idempotencyKey;
        return toPaymentResponse((LinkedHashMap<String, Object>)redisTemplate.opsForValue().get(key));
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

    public PaymentResponse toPaymentResponse(LinkedHashMap<String, Object> map) {

        if (map == null) return null;

        PaymentResponse pr = new PaymentResponse();

        pr.setTransactionId((String) map.get("transactionId"));
        pr.setIdempotencyKey((String) map.get("idempotencyKey"));

        // BigDecimal handling
        Object amount = map.get("amount");
        if (amount != null) {
            pr.setAmount(new BigDecimal(amount.toString()));
        }

        pr.setCurrency((String) map.get("currency"));

        // Enum handling
        Object status = map.get("status");
        if (status != null) {
            pr.setStatus(PaymentStatus.valueOf(status.toString()));
        }

        // LocalDateTime handling
        pr.setCreatedAt(parseDate(map.get("createdAt")));
        pr.setProcessedAt(parseDate(map.get("processedAt")));

        pr.setMessage((String) map.get("message"));

        return pr;
    }

    private LocalDateTime parseDate(Object value) {
        if (value == null) return null;

        if (value instanceof String) {
            return LocalDateTime.parse((String) value);
        }

        throw new IllegalArgumentException("Unsupported date format: " + value);
    }
}