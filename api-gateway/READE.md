✅ Production Gateway Pattern (Recommended)

GlobalFilter for:
* ✔ JWT validation
* ✔ correlation ID injection
* ✔ audit logging
* ✔ rate limit
* ✔ tenant resolution

Per-route filters for:
* ✔ path rewrite
* ✔ header add/remove
* ✔ retry/circuit breaker

```
client
 → api-gateway
    → user-service
       → (Feign) → project-bookmark
auth-service (JWT issuer)
discovery-service (registry)

```
#### use Resilience4j with Spring Boot

### **✅ Production Placement — What Goes Where**
#### 🔵 API Gateway Layer (edge protection)

Implement at api-gateway:

* ✅ RateLimiter
* ✅ CircuitBreaker (optional but recommended)
* ✅ TimeLimiter
* ❌ Retry (usually NO at edge)
* ❌ Bulkhead (not typical at edge)

Why: protect platform from traffic spikes & slow downstreams.

#### **🟢 Service → Service Calls (Feign layer)**

Implement at user-service → project-bookmark call

* ✅ CircuitBreaker
* ✅ Retry
* ✅ TimeLimiter
* ✅ Bulkhead
* ❌ RateLimiter (usually not needed internally)

Why: this is where cascading failures start.


Resilience Strategy:
* **At service layer (user-service):**
   * CircuitBreaker, Retry, Bulkhead applied to Feign calls.
   * TimeLimiter avoided (since Feign is blocking).
   * Fallbacks return safe defaults or user-friendly messages.
   * Retries only for read operations, not writes (to avoid duplicates).

* **At gateway layer (API-Gateway):**
  * CircuitBreaker, RateLimiter, TimeLimiter applied.
  * Retry and Bulkhead intentionally avoided (best practice).
  * Configurations defined per service (USER-SERVICE-CB, PROJECT-BOOKMARK-CB, etc.).
  * Planning to use RedisRateLimiter with KeyResolver for client identification.


#### **🟡 auth-service / discovery-service**

Do NOT add resilience patterns here normally.

Reason:

auth-service → short, critical calls

discovery-service → infra component



**✅ Pattern by Pattern — Correct Placement**

| Pattern        | Where                  | Why                     |
| -------------- | ---------------------- | ----------------------- |
| CircuitBreaker | Gateway + Feign caller | stop cascading failures |
| Retry          | Feign caller only      | safe controlled retries |
| TimeLimiter    | Gateway + Feign caller | cut hung calls          |
| RateLimiter    | Gateway only           | client abuse protection |
| Bulkhead       | Feign caller           | thread isolation        |


## ✅ user-service → project-bookmark (Feign)
### **CircuitBreaker**
```yaml
resilience4j.circuitbreaker.instances.bookmarkCB.slidingWindowSize=10
resilience4j.circuitbreaker.instances.bookmarkCB.failureRateThreshold=50
resilience4j.circuitbreaker.instances.bookmarkCB.waitDurationInOpenState=20s
resilience4j.circuitbreaker.instances.bookmarkCB.permittedNumberOfCallsInHalfOpenState=3

```
### **Retry**
```yaml
resilience4j.retry.instances.bookmarkRetry.maxAttempts=3
resilience4j.retry.instances.bookmarkRetry.waitDuration=500ms
```
### **TimeLimiter**
```yaml
resilience4j.timelimiter.instances.bookmarkTL.timeoutDuration=2s
```
### **Bulkhead**
```yaml
resilience4j.bulkhead.instances.bookmarkBH.maxConcurrentCalls=10
resilience4j.bulkhead.instances.bookmarkBH.maxWaitDuration=0
```
### **RateLimiter**
```yaml
resilience4j.ratelimiter.instances.bookmarkRL.limitForPeriod=10
resilience4j.ratelimiter.instances.bookmarkRL.limitRefreshPeriod=1s
```


### Use with Feign method
```java
@CircuitBreaker(name="bookmarkCB", fallbackMethod="fallback")
@Retry(name="bookmarkRetry")
@TimeLimiter(name="bookmarkTL")
@Bulkhead(name="bookmarkBH", type = Bulkhead.Type.THREADPOOL)
public CompletableFuture<List<Bookmark>> getBookmarks(...)

```
(TimeLimiter requires async return type.)


***
***

## ✅ API Gateway — RateLimiter + CircuitBreaker

Using Spring Cloud Gateway filters.

application.properties
```
spring.cloud.gateway.routes[0].filters[0]=RequestRateLimiter=redis-rate-limiter.replenishRate=10,redis-rate-limiter.burstCapacity=20
spring.cloud.gateway.routes[0].filters[1]=CircuitBreaker=name=userCB,fallbackUri=forward:/fallback

```
### ✅ OPTION B — Java DSL Config
Use when config must be dynamic or code-driven.
### ✅ user-service Java Config

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> cbCustomizer() {
    return factory -> factory.configure(builder ->
                    builder
                            .circuitBreakerConfig(
                                    CircuitBreakerConfig.custom()
                                            .failureRateThreshold(50)
                                            .slidingWindowSize(10)
                                            .waitDurationInOpenState(Duration.ofSeconds(20))
                                            .build())
                            .timeLimiterConfig(
                                    TimeLimiterConfig.custom()
                                            .timeoutDuration(Duration.ofSeconds(2))
                                            .build()),
            "bookmarkCB");
}
```

### ✅ Gateway Java DSL Filters
```
.route("bookmark", r -> r
    .path("/api/bookmarks/**")
    .filters(f -> f
        .requestRateLimiter(c -> c
            .setRateLimiter(redisLimiter()))
        .circuitBreaker(c -> c
            .setName("bookmarkCB")
            .setFallbackUri("forward:/fallback")))
    .uri("lb://PROJECT-BOOKMARK"))

```

### ✅ Recommended Production Strategy

✅ Use properties/YAML for:
* CircuitBreaker thresholds
* Retry counts
* Timeouts
* Rate limits

Reason:
* tunable without redeploy
* ops friendly
* environment-specific overrides

✅ Use Java DSL for:
* fallback routing logic
* dynamic route filters
* conditional resilience

## ✅ Golden Production Pattern for YOUR Setup
### api-gateway
* RateLimiter
* CircuitBreaker
* TimeLimiter

### user-service (Feign caller)
* CircuitBreaker
* Retry
* TimeLimiter
* Bulkhead

project-bookmark
* none (callee should stay clean)


### ✅ Important Anti-Pattern Warning
❌ Do NOT stack retries at:
* gateway
* feign
* client

→ retry storm amplification.
Retry only at one layer → caller service.

***
***

## Java DSL Config : Use when config must be dynamic or code-driven.
**"Config must be dynamic” means the resilience settings are decided or changed by code at runtime, instead of being fixed in application.properties or YAML at startup.**

* Static config = values are fixed unless you restart the service.
* Dynamic config = values can vary based on logic, environment, or live conditions.

### ✅ Static (properties/YAML) — fixed numbers
Example — always same timeout and failure threshold:
```yaml
resilience4j.circuitbreaker.instances.bookmarkCB.failureRateThreshold=50
resilience4j.timelimiter.instances.bookmarkTL.timeoutDuration=2s
```
Behavior never changes until restart.
Best for:
* production defaults
* ops tuning
* environment overrides

### Dynamic Config — Java DSL — decided by code
##### You compute config based on logic.
**Simple example — different timeout per environment**
```java
@Bean
Customizer<Resilience4JCircuitBreakerFactory> customizer() {
    boolean prod = System.getenv("ENV").equals("prod");

    Duration timeout = prod ? Duration.ofSeconds(2)
            : Duration.ofSeconds(10);

    return factory -> factory.configure(builder ->
                    builder.timeLimiterConfig(
                            TimeLimiterConfig.custom()
                                    .timeoutDuration(timeout)
                                    .build()
                    ),
            "bookmarkCB");
}

```
Same code → different behavior without editing properties.

### ✅ Another simple dynamic example
Different config for different downstream services:
```java
factory.configure(builder -> builder
    .circuitBreakerConfig(cbFast()), "fastService");

factory.configure(builder -> builder
    .circuitBreakerConfig(cbSlow()), "slowService");
```
#### ✅ When dynamic config is actually useful
Use Java DSL when:
* values depend on runtime conditions
* per-tenant or per-customer limits
* feature flags
* A/B testing resilience settings
* config loaded from database/config server programmatically

### ✅ Production rule of thumb
* **90% of cases → use properties/YAML**
* **Use Java DSL only when logic is required**

------
------
## RESILIENCE4J

1. **Resilience Patterns at Gateway**

* ✅ CircuitBreaker: Good at gateway level to stop flooding downstream services when they’re failing.
* ✅ RateLimiter: Correct at gateway level to protect downstream services from traffic spikes.
* ⚠️ TimeLimiter: Works only with async/reactive calls. Since Spring Cloud Gateway is reactive, you can use it here. But keep in mind: Feign (blocking) doesn’t benefit from TimeLimiter, whereas Gateway (reactive) does.
* ❌ Retry & Bulkhead: Correctly avoided at gateway. Retries belong in service layer, bulkheads belong in service layer too.

**CircuitBreaker (Gateway)** :
**Stops sending traffic when downstream failing.**
Use when:
* service is down
* error rate high
* response latency exploding

Effect:
Gateway fails fast → fallback → protects system.

**RateLimiter (Gateway)**

Protects against:
* client abuse
* traffic spikes
* DDoS-like bursts
* accidental load

Best placed at gateway — not per service.

**TimeLimiter (Gateway)**

Protects against:
* slow downstream services
* hung responses
* resource exhaustion

But again — prefer: in application.properties : `spring.cloud.gateway.httpclient.response-timeout`



2. Default vs Instance Config
    * If you define resilience4j.ratelimiter.configs.default, it applies to all routes unless overridden.
    * You’ve defined per‑service instances (PROJECT-BOOKMARK-RL, USER-SERVICE-RL) → this is better, because you can tune limits per service.

3. Route DSL
    * Right now, you’re only routing. To apply resilience filters, you need to attach Spring Cloud Gateway Resilience4j filters:
    * Similarly, you can attach rateLimiter and requestRateLimiter filters.
   ```java
   .filters(f -> f.circuitBreaker(c -> c.setName("USER-SERVICE-CB")
                                    .setFallbackUri("forward:/fallback/user")))
   ```

## **🔹 Explanation of Improvements**
* CircuitBreaker filter: Stops calls when downstream service is failing. fallbackUri lets you define a controller endpoint to return a friendly response.
* RateLimiter filter: Protects downstream services from traffic spikes. You linked it to your per‑service configs (USER-SERVICE-RL, PROJECT-BOOKMARK-RL).
* Fallback URIs: Instead of returning raw errors, you can forward to a Spring MVC/Reactive controller that returns JSON like:
* RateLimiter says: “Slow down — only allow N requests per time window.”


```java
@RestController
public class GatewayFallbackController {
    @RequestMapping("/fallback/user")
    public Mono<String> userFallback() {
        return Mono.just("User service is temporarily unavailable.");
    }

    @RequestMapping("/fallback/bookmark")
    public Mono<String> bookmarkFallback() {
        return Mono.just("Bookmark service is temporarily unavailable.");
    }
}

```

##### **✅ 5️⃣ Correct Gateway RateLimiter Implementation**

**Spring Cloud Gateway RateLimiter = RedisRateLimiter**
**Add dependency:** `spring-boot-starter-data-redis-reactive`

**Bean config**
```java
@Bean
public RedisRateLimiter userServiceRateLimiter() {
    return new RedisRateLimiter(10, 20);
}

@Bean
public RedisRateLimiter bookmarkRateLimiter() {
    return new RedisRateLimiter(10, 20);
}
meaning : 10 requests/sec
20 burst capacity
```
##### **Need KeyResolver**

**Defines per-user or per-IP throttling.**

**✅ Why KeyResolver is needed :**
**RateLimiter must answer this question:**

**“Rate limit WHO?”**

Because limits are applied per key.

That key could be:
* per IP address
* per logged-in user
* per API key
* per JWT subject
* per client app

👉 KeyResolver tells Gateway how to compute that key.

Example — per IP:
```java
@Bean
public KeyResolver ipKeyResolver() {
    return exchange ->
        Mono.just(exchange.getRequest()
            .getRemoteAddress()
            .getAddress()
            .getHostAddress());
}
```
HERE Key = client IP address So limits become: 10 requests/sec per IP
EXAMPLE :
| Client IP | Requests/sec | Result      |
| --------- | ------------ | ----------- |
| 10.1.1.5  | 8            | ✅ allowed   |
| 10.1.1.5  | 15           | ❌ throttled |
| 10.1.1.9  | 9            | ✅ allowed   |

✅ Why Redis is used with RateLimiter
* Redis is a distributed cache that can store and retrieve data quickly.
* It’s ideal for rate limiting because it can handle high traffic and provide consistent results across multiple instances of your API Gateway.
* Redis also supports distributed locking, which can help prevent race conditions and ensure that rate limits are applied consistently across multiple instances of your API Gateway.
* Redis is also highly scalable and can be easily distributed across multiple nodes.
* Redis also provides a simple and efficient way to store and retrieve rate limit data.
* KeyResolver answers: “Who gets their own rate limit bucket?” : Redis exists because Rate limits must be shared across all gateway instances.

###### **✅ When NOT to use per-IP limiting**

Per-IP breaks when:
* many users behind same NAT
* corporate proxy
* mobile carrier networks

Then one office = one IP = everyone throttled.

Better → userId from JWT.

Using a JWT-based KeyResolver that extracts the username (or subject) from the token already present in the Authorization header. This is the production-grade approach when your gateway validates JWT.

**REFACTORED ROUTING CODE :**
```java
@Bean
public RouteLocator customRoutes(RouteLocatorBuilder builder) {

    return builder.routes()

        // USER SERVICE ROUTE
        .route("user_service", r -> r
            .path("/api/users/**")
            .filters(f -> f
                .circuitBreaker(c -> c
                    .setName("USER-SERVICE-CB")
                    .setFallbackUri("forward:/api/fallback/userServiceFallback"))
                .requestRateLimiter(rl -> rl
                    .setRateLimiter(userServiceRateLimiter())
                    .setKeyResolver(ipKeyResolver())
                )
            )
            .uri("lb://USER-SERVICE"))

        // BOOKMARK SERVICE ROUTE
        .route("bookmark_service", r -> r
            .path("/api/bookmarks/**")
            .filters(f -> f
                .circuitBreaker(c -> c
                    .setName("PROJECT-BOOKMARK-CB")
                    .setFallbackUri("forward:/api/fallback/bookmarkServiceFallback"))
                .requestRateLimiter(rl -> rl
                    .setRateLimiter(bookmarkRateLimiter())
                    .setKeyResolver(ipKeyResolver())
                )
            )
            .uri("lb://PROJECT-BOOKMARK"))

        .build();
}

```



##### **✅ 7️⃣ Production Improvements Checklist**

**Gateway Layer**

1. ✅ Use RedisRateLimiter
2. ✅ Use httpclient timeout instead of TL
3. ✅ CircuitBreaker per route
4. ✅ No retry
5. ✅ No bulkhead
6. ✅ Per-IP or per-user KeyResolver
7. ❌ Resilience4j TimeLimiter (not automatically wired to routes)

##### **✅ When TimeLimiter does make sense**

Use Resilience4j TimeLimiter when:

* You are calling async methods
* Using @TimeLimiter on service methods
* Using Feign / RestTemplate / WebClient in services
* You want per-method timeout + fallbackk
* Prefer Gateway httpclient timeout instead AT API-GATEWAY NOT AT SERVICE LAYER OR DOWNSTREAM SERVICES
Example (service layer):
```java
@TimeLimiter(name="USER-SERVICE-TL", fallbackMethod="fallback")
public CompletableFuture<Data> callService() { ... }
```
That’s where your resilience4j.timelimiter.instances.* config is applied.

##### **Service Layer (Feign)**

1. ✅ CircuitBreaker
2. ✅ Retry
3. ✅ Bulkhead
4. ✅ TimeLimiter



✅ Final Verdict on Your Design
| Layer               | Your Choice          | Verdict    |
| ------------------- | -------------------- | ---------  |
| Gateway             | CB + RL + TL         | ✅ Correct |
| User-Service        | CB + Retry + BH + TL | ✅ Correct |
| Retry at Gateway    | Not used             | ✅ Correct |
| Bulkhead at Gateway | Not used             | ✅ Correct |


Production setup step-by-step using Spring Cloud Gateway + YAML routes + Redis RateLimiter.

* ✅ Step 1 — Create Username-from-JWT KeyResolver Bean : RateLimitKeyResolverConfig
This bean extracts username (subject) from JWT and becomes the rate-limit key.
```java
@Configuration
public class RateLimitKeyResolverConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.just("anonymous");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        //.parserBuilder()
                        .verifyWith(
                                Keys.hmacShaKeyFor(
                                        Base64.getDecoder().decode(JwtUtil.SECRET_KEY)
                                )
                        )
                        /*.setSigningKey(
                                Keys.hmacShaKeyFor(
                                        Base64.getDecoder().decode(JwtUtil.SECRET_KEY)
                                )
                        )*/
                        .build()
                        //.parseClaimsJws(token)
                        .parseSignedClaims(token)
                        //.getBody();
                        .getPayload();

                String username = claims.get("username").toString(); // usually username
                return Mono.just(username);

            } catch (Exception e) {
                return Mono.just("invalid-token");
            }
        };
    }
}
```
* ✅ Step 2 — Correct YAML RateLimiter Config
```yaml
- id: bookmark_service
  uri: lb://BOOKMARK-SERVICE
  predicates:
     - Path=/api/bookmarks/**
  filters:
     - AuthFilter

     - name: RequestRateLimiter
       args:
          key-resolver: "#{@userKeyResolver}"
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20

     - name: CircuitBreaker
       args:
          name: PROJECT-BOOKMARK-CB
          fallbackUri: forward:/api/fallback/bookmarkServiceFallback

```

* ✅ Step 3 — Redis Dependency Required
Gateway RateLimiter requires Redis. Without it → startup failure.
Add dependency: `spring-boot-starter-data-redis-reactive`

* ✅ Step 4 — Redis Connection Config
Configure Redis connection in application.properties or YAML.
* this Redis configuration is mandatory if you want to use the Redis-based RateLimiter in Spring Cloud Gateway.
* Redis must be configured and reachable. Otherwise the gateway will fail at startup or throw runtime errors when handling requests.
```properties
#Redis Connection Config
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2s
```
✅ Step 5 — What Each Setting Does
replenishRate: 10   → Add 10 tokens per second per user
burstCapacity: 20   → Allow 20 requests at once before throttling. → Allow short burst up to 20 requests

```css
User A → 10 req/sec steady
User A → burst spike allowed to 20
User B → separate bucket
```
Because key = username from JWT.

✅ Step 6 — Filter Execution Order (Production Tip)
```css
AuthFilter
RateLimiter
CircuitBreaker
```
You already follow this — good.

✅ Step 7 — When You Should Use Per-User vs Per-IP:
Use JWT username resolver when:
* ✅ authenticated APIs
* ✅ multi-tenant systems
* ✅ fairness required

Use IP resolver when:
* public APIs
* no authentication
* bot control





















































