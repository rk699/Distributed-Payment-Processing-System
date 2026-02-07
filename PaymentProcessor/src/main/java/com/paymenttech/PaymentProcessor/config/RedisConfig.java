package com.paymenttech.PaymentProcessor.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

//    @Cacheable
//   ↓
//    CacheManager
//   ↓
//    RedisCacheConfiguration
//   ↓
//    Redis


    // 1. RedisConnectionFactory manages Redis connections and is the backbone
    //      for RedisTemplate and Spring Cache operations.
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    // 2. RedisTemplate bean (important!)
    //Redis stores bytes, not Java object
    //RedisTemplate converts:
    //Java Object ↔ Bytes ↔ Redis
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

//        Uses Lettuce connection pool
//        Thread-safe, reusable connections
        template.setConnectionFactory(factory);

//        Handles LocalDateTime, Instant
//        Avoids unreadable timestamps
//        Ensures Redis data is human-readable
//
//        Without this → Jackson errors or ugly data
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);//Converts any Java object → JSON

        // Key serializer
//        Redis keys should be readable
//        Prevents garbage byte keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        // Initialize template
        template.afterPropertiesSet();
//        Finalizes configuration
//
//        Mandatory for proper initialization
        return template;
    }

//    👉 This is NOT RedisTemplate
//👉 This is Spring Cache behavior config
    // 3. Cache configuration
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))//Cache expires after 30 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class))
                );

//        Redis keys must be:
//        Human readable
//        Stable
//        Predictable
    }


//    @Cacheable does not talk to Redis directly
//    Spring needs a manager to:
//    Decide where to cache
//    Apply rules (TTL, serializer)
//    Create cache regions
//    No CacheManager → caching fails silently
    // 4. Cache manager using our custom configuration
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(cacheConfiguration()) // ✅ Important
                .build();

//        Applies:
//        TTL
//        Key serializer
//        Value serializer
//        Finalizes configuration
//        Registers bean in Spring context
    }
}
