package com.learncicd.authservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 *
 * When you define a custom RedisCacheManager bean, you can configure different TTLs for different caches and
 * the TTL you set with .entryTtl(Duration.ofMinutes(10)) becomes the default TTL for all caches managed by that bean.
 *
 * In that case, the property spring.cache.redis.time-to-live in application.properties is ignored,
 * because your explicit bean configuration overrides the auto‑configuration.
 *
 * If you only need one global TTL, properties are simpler — no need for a custom bean.
 *
 * If you want different TTLs per cache (e.g., users cache = 10 minutes, projects cache = 1 hour),
 * then a custom RedisCacheManager bean is the right approach.
 *
 */
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // default TTL
         .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
