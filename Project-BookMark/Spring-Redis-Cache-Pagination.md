## Spring’s @Cacheable abstraction

## 🔹 Why Redis for Pagination
Pagination queries often hit the same pages repeatedly (e.g., page 1, page 2).

Caching those results avoids repeated DB calls.

Redis is perfect for this because it’s fast, distributed, and integrates smoothly with Spring Cache.

1. **Add Dependencies**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-cache</artifactId>
</dependency>

```
2. **Enable Caching :**    In your main application class:
```java
@SpringBootApplication
@EnableCaching
public class BookmarkApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookmarkApplication.class, args);
    }
}

```
3. **Configure Redis :**   In application.yml:
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379

```
4. **Apply Caching in Service :**
   You want to cache paged results. Since pagination depends on pageNo and pageSize, use them as part of the cache key.
```java
@Service
@Transactional(readOnly = true)
public class BookmarkService {
    private final BookmarkRepository repo;

    BookmarkService(BookmarkRepository repo) {
        this.repo = repo;
    }

    @Cacheable(value = "bookmarks", key = "#query.pageNo() + '-' + #query.pageSize()")
    public PagedResult<BookmarkDTO> findBookmarks(FindBookmarksQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);

        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public BookmarkDTO create(CreateBookmarkCommand cmd) {
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        return BookmarkDTO.from(repo.save(bookmark));
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public void update(UpdateBookmarkCommand cmd) {
        Bookmark bookmark = repo.findById(cmd.id())
                .orElseThrow(() -> BookmarkNotFoundException.of(cmd.id()));
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        repo.save(bookmark);
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public void delete(Long postId) {
        Bookmark entity = repo.findById(postId)
                .orElseThrow(() -> BookmarkNotFoundException.of(postId));
        repo.delete(entity);
    }
}

```

5.**🔹 Key Points** :
* @Cacheable caches paginated results by page number and size.
* @CacheEvict(allEntries = true) clears cache whenever data changes (create/update/delete).
* This ensures cache consistency.
* You can fine‑tune TTL (time-to-live) in Redis to auto‑expire old pages.

6. **Optional: Set TTL for Cache Entries** : In application.yml:
```yaml
spring:
  cache:
    redis:
      time-to-live: 600000 # 10 minutes
```

✅ With this setup:
* First request for /api/bookmarks?page=1&size=10 hits DB and caches result.
* Subsequent requests for the same page fetch directly from Redis.
* Any create/update/delete clears the cache so fresh data is loaded next time.


***
***

# **CACHING : RedisTemplate-based approach**
This gives full control over cache keys, TTLs, and serialization, instead of relying on Spring’s @Cacheable abstraction.

1. **Configure RedisTemplate** :
   Create a configuration class to expose a RedisTemplate<String, Object> bean:
```java
@Configuration
public class RedisConfig {

   @Bean
   public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
      RedisTemplate<String, Object> template = new RedisTemplate<>();
      template.setConnectionFactory(connectionFactory);

      // Use String keys
      template.setKeySerializer(new StringRedisSerializer());

      // Use JSON for values
      template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

      return template;
   }
}

```

2. **Service Layer with Manual Caching** :
   Here’s how you can integrate Redis caching into your BookmarkService:
```java
@Service
@Transactional(readOnly = true)
public class BookmarkService {
    private final BookmarkRepository repo;
    private final RedisTemplate<String, Object> redisTemplate;

    BookmarkService(BookmarkRepository repo, RedisTemplate<String, Object> redisTemplate) {
        this.repo = repo;
        this.redisTemplate = redisTemplate;
    }

    public PagedResult<BookmarkDTO> findBookmarks(FindBookmarksQuery query) {
        String cacheKey = "bookmarks:page:" + query.pageNo() + ":size:" + query.pageSize();

        // Try cache first
        PagedResult<BookmarkDTO> cachedResult = (PagedResult<BookmarkDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // If not cached, query DB
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);

        PagedResult<BookmarkDTO> result = new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );

        // Store in Redis with TTL
        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(10));

        return result;
    }

    @Transactional
    public BookmarkDTO create(CreateBookmarkCommand cmd) {
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        BookmarkDTO dto = BookmarkDTO.from(repo.save(bookmark));

        // Invalidate all cached pages
        clearBookmarkCache();

        return dto;
    }

    @Transactional
    public void update(UpdateBookmarkCommand cmd) {
        Bookmark bookmark = repo.findById(cmd.id())
                .orElseThrow(() -> BookmarkNotFoundException.of(cmd.id()));
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        repo.save(bookmark);

        clearBookmarkCache();
    }

    @Transactional
    public void delete(Long postId) {
        Bookmark entity = repo.findById(postId)
                .orElseThrow(() -> BookmarkNotFoundException.of(postId));
        repo.delete(entity);

        clearBookmarkCache();
    }

    private void clearBookmarkCache() {
        // Delete all keys matching bookmarks:page:*
        Set<String> keys = redisTemplate.keys("bookmarks:page:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}


```

3. **WE CAN CREATE A CUSTOM REDIS-SERVICE CLASS TO SET AND GET CACHE VALUES.**
```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylearning.journalapp.clientexception.PersonCallingClientException;
import com.mylearning.journalapp.exception.PersonJsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    public <T>T get(String key,Class<T> classType) {
        try {
            Object objectResponse = redisTemplate.opsForValue().get(key);
            if (objectResponse != null) return objectMapper.readValue(objectResponse.toString(), classType);
            else throw PersonJsonProcessingException.personException("Exception Occurred In Get RedisService Unable To GET READ VALUE From REDIS Key");
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("RedisService get Exception Occurred : {}", e.getMessage());
            log.error("RedisService get going to return null");
            return null;
        }
    }

    public void set(String key, Object value, Long ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key,jsonValue,ttl, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("RedisService set Exception Occurred  Unable to SET VALUE TO REDIS: {}", e.getMessage());

        }
    }
}
```


4. **Key Points**
* Cache Key: bookmarks:page:{pageNo}:size:{pageSize} ensures uniqueness per pagination request.
* TTL: Entries expire after 10 minutes (Duration.ofMinutes(10)), so stale data is automatically removed.
* Eviction: On create/update/delete, we clear all cached pages to keep data consistent.
* Flexibility: You can add more metadata to keys (e.g., userId, filters) if needed.

✅ With this approach, you have fine-grained control: you decide when to cache, when to evict, and how long entries live.


***
***

## **combining RedisTemplate with a custom CacheManager** 
combining RedisTemplate with a custom CacheManager lets you mix manual caching (fine‑grained control) with Spring’s @Cacheable abstraction (declarative caching). This hybrid approach is powerful for complex systems.

1. **Configure Redis CacheManager + RedisTemplate**:
```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // default TTL
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}

```
* RedisCacheManager: used by @Cacheable, @CacheEvict, etc.
* RedisTemplate: used for manual caching with custom keys.


2. **Service Layer Mixing Both Approaches** :
```java
@Service
@Transactional(readOnly = true)
public class BookmarkService {
    private final BookmarkRepository repo;
    private final RedisTemplate<String, Object> redisTemplate;

    BookmarkService(BookmarkRepository repo, RedisTemplate<String, Object> redisTemplate) {
        this.repo = repo;
        this.redisTemplate = redisTemplate;
    }

    // Declarative caching with @Cacheable
    @Cacheable(value = "bookmarks", key = "#query.pageNo() + '-' + #query.pageSize()")
    public PagedResult<BookmarkDTO> findBookmarks(FindBookmarksQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);

        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    // Manual caching example
    public PagedResult<BookmarkDTO> findBookmarksManual(FindBookmarksQuery query) {
        String cacheKey = "bookmarks:page:" + query.pageNo() + ":size:" + query.pageSize();

        PagedResult<BookmarkDTO> cached = (PagedResult<BookmarkDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);

        PagedResult<BookmarkDTO> result = new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );

        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));
        return result;
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public BookmarkDTO create(CreateBookmarkCommand cmd) {
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        BookmarkDTO dto = BookmarkDTO.from(repo.save(bookmark));

        clearManualCache();
        return dto;
    }

    private void clearManualCache() {
        Set<String> keys = redisTemplate.keys("bookmarks:page:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}

```
3. When to Use Which
* @Cacheable:
   * Simple, declarative caching for common queries.
   * Works well for pagination where keys are predictable.
* RedisTemplate:
   * Use when you need custom keys, fine‑grained TTLs, or complex eviction strategies.
   * Example: caching filtered queries (bookmarks:user:123:page:1:size:10).

4. Hybrid Strategy
* Use @Cacheable for standard pagination.
* Use RedisTemplate for advanced scenarios (filters, user‑specific queries, bulk eviction).
* Both share the same Redis instance, so you can mix them seamlessly.


***
***
## **Cache Warming**
1. **Why Cache Warming**
* Avoids “cold start” latency when the app first boots.
* Ensures frequently accessed pages (like page 1, size 10) are already cached.
* Useful for dashboards, landing pages, or common queries.

2. **Implement Cache Warming with ApplicationRunner** :
   You can hook into Spring Boot’s startup lifecycle and pre‑load Redis with data:

```java
@Component
public class BookmarkCacheWarmup implements ApplicationRunner {

    private final BookmarkService bookmarkService;
    private final RedisTemplate<String, Object> redisTemplate;

    public BookmarkCacheWarmup(BookmarkService bookmarkService, RedisTemplate<String, Object> redisTemplate) {
        this.bookmarkService = bookmarkService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Define popular pages to warm up
        List<FindBookmarksQuery> warmupQueries = List.of(
                new FindBookmarksQuery(1, 10),
                new FindBookmarksQuery(1, 20),
                new FindBookmarksQuery(2, 10)
        );

        for (FindBookmarksQuery query : warmupQueries) {
            String cacheKey = "bookmarks:page:" + query.pageNo() + ":size:" + query.pageSize();

            if (!Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                PagedResult<BookmarkDTO> result = bookmarkService.findBookmarks(query);
                redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(10));
                System.out.println("Cache warmed for " + cacheKey);
            }
        }
    }
}

```

3. **Key Points**
*   Runs at startup: ApplicationRunner executes after the app context is ready.
*   Warmup queries: You decide which pages to pre‑load (e.g., first 2–3 pages).
*   TTL: Entries expire naturally after 10 minutes (or whatever you configure).
*   Safety: Checks hasKey before warming to avoid redundant DB hits.

4. **Advanced Options** :
* Dynamic warmup: Load top N pages based on analytics (e.g., most requested).
* Scheduled refresh: Use @Scheduled to periodically refresh cache entries.
* Multi‑tenant warmup: Pre‑load per user or per filter if your app supports personalization..



















