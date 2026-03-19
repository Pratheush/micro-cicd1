
1. **API Gateway: Should only enforce coarse-grained RBAC (role-based access control). It decides which roles can reach which endpoints.**
2. **Downstream Services (User Service, Project-Bookmark Service): Should enforce fine-grained PBAC/ABAC (policy-based or attribute-based access control). This is where ownership checks, resource-specific rules, and method-level annotations belong.**

### @Secured :
* Use in service layer methods (UserService, BookmarkService).
* Best for simple role checks before executing business logic.
* Example in UserService:
```java
@Secured("ROLE_ADMIN")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```
→ Only ADMIN can delete users.

### @PreAuthorize :
* Use in controller or service methods where you need flexible, pre-execution checks.
* Supports SpEL (Spring Expression Language), so you can check ownership or multiple roles.
* Example in **Project-Bookmark Service**::
```java
@PreAuthorize("hasRole('ROLE_ADMIN') or #bookmark.createdBy == authentication.name")
public void deleteBookmark(Bookmark bookmark) {
    repository.delete(bookmark);
}

```
→ ADMIN can delete any bookmark, USER can delete only their own.

### @PostAuthorize : 
* Use when the decision depends on the returned object.
* Example in Project-Bookmark Service:
```java
@PostAuthorize("returnObject.createdBy == authentication.name or hasRole('ROLE_ADMIN')")
public Bookmark getBookmark(Long id) {
    return repository.findById(id).orElseThrow();
}

```
→ Method runs, returns a bookmark, then Spring checks if the caller is the owner or ADMIN.

#### **Project-Bookmark Service** :
* **This is where PBAC (policy-based access control) belongs.**
* **Use @PreAuthorize and @PostAuthorize for ownership checks:**
  * Example: @PreAuthorize("hasRole('ROLE_ADMIN') or #bookmark.createdBy == authentication.name") on updateBookmark().
  * Example: @PostAuthorize("returnObject.createdBy == authentication.name or hasRole('ROLE_ADMIN')") on getBookmark().
* **This ensures that even if Gateway lets a request through, the service itself enforces resource-level rules.**


## 🎯 Best Practice (Production Grade)
1. [x] Gateway → RBAC only. Keep it simple.
2. [x] User Service → @Secured or @PreAuthorize for role restrictions on user management.
3. [x] Project-Bookmark Service → @PreAuthorize / @PostAuthorize for PBAC (ownership checks).
4. [x] Never rely solely on Gateway for security — always enforce PBAC inside resource services.
5. [x] Use @PreAuthorize over @Secured in most cases because it supports SpEL (ownership checks, multiple roles). @Secured is fine for simple role-only methods.

### **🔎 Why You Need JwtAuthenticationConverter**
1. By default, Spring Security’s JWT support only maps the scope or scp claim into authorities (e.g., SCOPE_read).
2. If your JWT contains custom claims like roles or authorities, Spring won’t automatically map them into GrantedAuthority.
3. Without mapping, authentication.getAuthorities() will be empty or incorrect.
4. That means @Secured("ROLE_ADMIN") or @PreAuthorize("hasRole('ROLE_USER')") won’t work — they’ll always deny access.

### **configure a JwtAuthenticationConverter bean**
1. Reads your JWT claims (roles, authorities).
2. Converts them into GrantedAuthority objects (ROLE_USER, ROLE_ADMIN, etc.).
3. Attaches them to the Authentication object in the SecurityContext.
EXAMPLE IN NON-REACTIVE : 
```java
@Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtConverter;
    }
```
EXAMPLE IN REACTIVE : LIKE API-GATEWAY
```java
@Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");   // important: no ROLE_ prefix

        ReactiveJwtAuthenticationConverter reactiveConverter = new ReactiveJwtAuthenticationConverter();

        reactiveConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                {
                    Collection<GrantedAuthority> authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
                    String role= jwt.getClaimAsString("roles");
                    if(role!=null){
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                    return Flux.fromIterable(authorities);
                }
        );

        return reactiveConverter;
    }
```

Then in your security config:
```java
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("SecurityConfig API Request: Security Filter Chain HttpSecurity={}", http );

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Register Access Denied Handler Bean:
                .exceptionHandling(exceptionHandlingSpec ->
                        exceptionHandlingSpec.accessDeniedHandler(accessDeniedHandler()))
                // Register Authentication Entry Point Handler Bean:
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                        .authorizeExchange(exchange -> exchange

                                // ✅ Correct Ordering
                                //Always define from most privileged to least:
                                        /**
                                         * 🎯 Best Practice
                                         * Keep public endpoints separate with .permitAll().
                                         * Use .hasAnyRole() for shared access paths.
                                         * Reserve .hasRole() for exclusive access (like POST → USER only).
                                         * Avoid repeating the same path multiple times with different roles — that’s what caused the 403 issue.
                                         *  replace the individual role matchers with these consolidated .hasAnyRole() rules. It will Prevent ordering conflicts.
                                         */

                                // Public endpoints
                                .pathMatchers("/auth/register-user","/auth/generate-token","/eureka/web","/eureka/**").permitAll()

                                        /**
                                         * Rule ordering matters
                                         * Spring Security processes rules in the order you declare them. The first match wins.
                                         * So if you put .hasRole("USER") before .hasRole("ADMIN"), then an ADMIN request to /api/bookmarks/**
                                         * matches the USER rule first. Since the token doesn’t contain ROLE_USER, access is denied — even though
                                         * an ADMIN rule exists later.
                                         */
                                // USER → full access (GET, PUT, DELETE)
//                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.POST,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")

                                        /**
                                         * uncomment this block of code if you want to use RoleHierarchy Bean that we created here at downside
                                         * uncomment RoleHierarchy too to use this block of code for matching rule :
                                         */
                                .pathMatchers(BOOKMARK_LINKS,USER_LINKS).hasRole("USER")

                                // ADMIN → full access (GET, PUT, DELETE)
                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")
                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")
                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")

                                // TL → READ + WRITE + UPDATE (GET, PUT)
                                .pathMatchers(HttpMethod.GET, BOOKMARK_LINKS,USER_LINKS).hasRole("TL")
                                .pathMatchers(HttpMethod.PUT, BOOKMARK_LINKS,USER_LINKS).hasRole("TL")

                                // JR → only READ (GET)
                                .pathMatchers(HttpMethod.GET, BOOKMARK_LINKS,USER_LINKS).hasRole("JR")

                                /*// GET → accessible by all roles (USER, ADMIN, TL, JR).
                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN","TL","JR")
                                // PUT → accessible by USER, ADMIN, TL.
                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN","TL")
                                // DELETE → accessible by USER, ADMIN.
                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN")
                                // POST → accessible only by USER.
                                .pathMatchers(HttpMethod.POST,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")*/

                                // Any other request must be authenticated
                                .anyExchange().authenticated()
                        ).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                            jwt.jwtDecoder(jwtDecoder());
                            jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()); // JwtAuthenticationConverter is added

                }));
        return http.build();
    }
```































