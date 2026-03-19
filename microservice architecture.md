## 🧠 Architectural Recommendation (Serious Microservice Design)
**In distributed systems:**
1. [ ] ✔ Gateway → coarse-grained access control
2. [ ] ✔ Resource service → fine-grained access control with `@PreAuthorize`

So best practice:
1. **At Gateway: Role based access control (RBAC) :**

use roles:
```java
.hasRole("ADMIN")
.hasRole("USER")
```

2. **At Resource Service: Permission/Policy based access control (PBAC)**

So at Bookmark service: Use authorities
```java
@PreAuthorize("hasAuthority('BOOKMARK_READ')")
```
That gives us layered security.

















































































































