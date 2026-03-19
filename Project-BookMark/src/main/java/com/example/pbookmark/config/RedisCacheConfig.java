package com.example.pbookmark.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Use a JSON serializer instead of JDK serialization
 * A more robust approach is to configure Redis to use Jackson JSON serialization.
 * That way, you don’t need to mark every class Serializable.
 *
 */
//@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registeredModules(); // registers JavaTimeModule, JDK8 modules, etc.
        return RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(mapper)
                        ));
    }
}
