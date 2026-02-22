### Eureka Dashboard Error :
**EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT. RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.**

🔎 Why This Happens
* Eureka server tracks heartbeats (renewals) from clients.
* If the number of renewals received in a given time window is less than the expected threshold, Eureka assumes there may be a network partition or mass outage.
* To avoid mistakenly expiring healthy instances during such events, Eureka enters a self-preservation mode.
* In this mode, instances are not expired, even if they stop sending heartbeats. That’s why you see “instances are UP when they’re not.”

How to Confirm (Immediately)
* Check Eureka dashboard or /actuator/health:
* Self preservation mode is ON
* Renews threshold not met
* Renews last min < threshold

This confirms the behavior.
✅ How to Handle It
1. Check Self-Preservation Mode
   * By default, it’s enabled. You can disable it if you prefer strict expiration:
```yaml
eureka:
  server:
    enable-self-preservation: false
```
But be careful — disabling can cause mass eviction during temporary network glitches.

2. Tune Renewal Thresholds
    * Eureka calculates expected renewals based on the number of registered instances.
    * You can adjust:
```yaml
eureka:
  server:
    renewal-percent-threshold: 0.85

```
(default is 0.85 → 85% of expected renewals must arrive, otherwise self-preservation kicks in).

3. Monitor Heartbeats
    * Ensure clients are sending renewals at the right interval:
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90

```
If clients are misconfigured (too long renewal interval), Eureka will think they’re unhealthy.

4. Add Circuit Breakers / Retries
    * Use Spring Cloud Netflix Hystrix or Resilience4j so that if Eureka returns a dead instance, your client retries another one.

🚨 Immediate Action Checklist
1. Verify client configs: lease-renewal-interval and lease-expiration-duration.
2. Check Eureka server logs for self-preservation mode activation.
3. Decide: Do you want strict eviction (disable self-preservation) or safety-first (keep it on but tune thresholds)?
4. Add monitoring/alerts on renewal counts vs threshold.

👉 My recommendation: keep self-preservation ON, but tune thresholds and make sure clients are configured correctly. Disabling it is risky unless you’re confident in your network stability.


***
***
### CIRCUITBREAKER USAGE :
```java
/**
     * In Resilience4j Annotations bind to instances, not configs settings from application.properties
     * FOR TIMELIMITER : Fails fast if a call takes too long. Requires async return type (CompletableFuture).
     * Use SEMAPHORE for synchronous Feign (correct in your code). THREADPOOL bulkhead is better for async clients.
     * @param pageNo
     * @param pageSize
     * @return
     */
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="fallback")
    @Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    //@TimeLimiter(name="USER-PROJECT-BOOKMARK-TL")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    @GetMapping
    PagedResult<BookmarkDTO> findBookmarks(
            @RequestParam(name = "page", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "size", defaultValue = "10") Integer pageSize);

    // In Fallback method : Signature must match method + Throwable last param.
    default PagedResult<BookmarkDTO> fallback(Integer pageNo,
                                              Integer pageSize,
                                              Throwable t) {
        return PagedResult.empty();
    }
```
* Feign Client Interface: When we put annotations directly on Feign client methods (like we did), Resilience4j wraps those calls automatically.
* This means every time you invoke bookmarkClient.findBookmarks(), the circuit breaker, retry, and bulkhead logic is applied transparently.
* Service Layer Methods: we can annotate the service methods that call the Feign client but this gives us more control we can decide which business operations should be resilient, rather than applying resilience to every Feign call.

### Which Approach to Use?
* Annotations on Feign client methods → simple, declarative, applies resilience to all calls automatically.
* Annotations on service methods → more flexible, lets you tailor resilience per use case, combine multiple Feign calls, or enrich fallbacks with business logic.

## 🔹 Best Practice Recommendation
* For production-grade systems, it’s usually better to annotate service methods rather than Feign interfaces.

   * Reason: You often want different resilience behavior depending on the business context (e.g., retries for reads, but not for writes).

   * Fallbacks are easier to implement in service classes, since you can add richer logic (like cached data, default responses, or logging).

* Annotating Feign client methods is fine for quick setups, but service-layer annotations give you more control and clarity.

❌ we cannot apply @CircuitBreaker, @Retry, @Bulkhead at class level.


### Centralized Config: Instead of hardcoding annotation names, you can externalize configs in application.properties (you already did this well).
![Screenshot 2026-02-17 142921.png](Screenshot%202026-02-17%20142921.png)
**CENTRALIZED CONFIG IS BETTER APPROACH :**
![Screenshot 2026-02-17 142954.png](Screenshot%202026-02-17%20142954.png)



