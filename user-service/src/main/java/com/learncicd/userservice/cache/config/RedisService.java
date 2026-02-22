package com.learncicd.userservice.cache.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T>T get(String key,Class<T> classType) {
        try {
            Object objectResponse = redisTemplate.opsForValue().get(key);
            if (objectResponse != null) return objectMapper.readValue(objectResponse.toString(), classType);
            else throw BookmarkJsonProcessingException.bookmarkException("Exception Occurred In Get RedisService Unable To GET READ VALUE From REDIS Key");
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("RedisService get Exception Occurred : {}", e.getMessage());
            log.error("RedisService get going to return null");
            return null;
        }
    }

    /*public void set(String key, Object value, Long ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key,jsonValue,ttl, TimeUnit.MINUTES);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("RedisService set Exception Occurred  Unable to SET VALUE TO REDIS: {}", e.getMessage());

        }
    }*/

    /**
     * Generic GET using TypeReference to support generic types
     */
    public <T> T get(String key, TypeReference<T> typeRef) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached == null) {
                log.debug("Redis MISS key={}", key);
                return null;
            }

            // serializer already stored JSON → convert back
            return new ObjectMapper().convertValue(cached, typeRef);

        } catch (Exception e) {
            log.error("Redis GET failed key={} error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Store object directly — serializer handles JSON
     */
    public void set(String key, Object value, Long ttlMinutes) {
        try {
            redisTemplate.opsForValue()
                    .set(key, value, ttlMinutes, TimeUnit.MINUTES);

            log.debug("Redis SET key={} ttl={}min", key, ttlMinutes);

        } catch (Exception e) {
            log.error("Redis SET failed key={} error={}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}