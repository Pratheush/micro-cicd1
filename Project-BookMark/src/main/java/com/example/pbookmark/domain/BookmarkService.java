package com.example.pbookmark.domain;

import com.example.pbookmark.domain.xception.AccessDeniedException;
import com.example.pbookmark.domain.xception.BookmarkNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 *
 * BookmarkService PBAC(Policy Based Access Control) :
 * - Create bookmark → only ROLE_USER.
 * - Read bookmark → any role, but PBAC ensures USER only sees their own.
 * - Delete bookmark → allowed if USER owns it, or ADMIN.
 * - Update bookmark → allowed for ADMIN, TL, or USER who owns it.
 * Api-Gateway enforces role categories. BookmarkService enforces ownership/resource rules.
 * using @PreAuthorize for ownership checks is a right choice.
 *
 * 🎯 Best Practice (Production Grade):
 * - Gateway → RBAC only. Keep it simple.
 * - User Service → @Secured or @PreAuthorize for role restrictions on user management.
 * - Project-Bookmark Service → @PreAuthorize / @PostAuthorize for PBAC (ownership checks).
 * - Never rely solely on Gateway for security — always enforce PBAC inside resource services.
 * - Use @PreAuthorize over @Secured in most cases because it supports SpEL (ownership checks, multiple roles). @Secured is fine for simple role-only methods.
 * - BookmarkService → PBAC + ownership checks. More secure.
 *
 *The BookmarkService is a transactional service layer component which will be used by
 * web layer or other service layer components. The BookmarkService class is annotated with
 * @Transactional(readOnly = true) which means all the public methods are transactional
 * and allows only read-only operations on the database.
 * We can override this read-only behaviour for the methods which needs to perform
 * insert/update/delete database operations by adding @Transactional annotation.
 *
 * @Cacheable caches paginated results by page number and size.
 * @CacheEvict(allEntries = true) clears cache whenever data changes (create/update/delete).
 * This ensures cache consistency.
 *
 * hasAuthority() is part of Spring Security’s expression language (SpEL) used in:
 * @PreAuthorize
 * @PostAuthorize
 * @Secured
 * it is derived from the JWT, but not directly from your Jwt parameter. It comes from the Spring Security authentication object created during request filtering.
 * It is evaluated by: MethodSecurityExpressionHandler which checks the current: SecurityContextHolder.getContext().getAuthentication()
 *
 * Where Does Authentication Come From?
 * Because you're using JWT, you likely configured: .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt) that enables JwtAuthenticationFilter
 * Flow:
 * Request comes with Authorization: Bearer <token>
 * Spring validates token
 * Spring creates a: JwtAuthenticationToken
 * That token contains: Principal (Jwt) , Authorities (Collection<GrantedAuthority>) , Authenticated = true
 * That object is stored inside: SecurityContextHolder
 *
 * 8️⃣ Correct Architecture Model ::
 * JWT
 *    ↓
 * JwtDecoder
 *    ↓
 * JwtAuthenticationConverter
 *    ↓
 * JwtAuthenticationToken
 *    ↓
 * SecurityContextHolder
 *    ↓
 * @PreAuthorize evaluates against Authentication
 *
 *
 * 9️⃣ Why ROLE_USER Check Works But Authority Fails
 * If you have: hasRole('USER') Spring automatically adds prefix: ROLE_ >> So it checks: ROLE_USER
 * But hasAuthority() does NOT auto-prefix anything. It must match exactly.
 *
 * You have: "roles": "ROLE_ADMIN" & If you're trying: hasRole('ADMIN') Spring expects authority: ROLE_ADMIN
 * But again — only if it's mapped into Authentication.
 *
 * hasAuthority come from > Spring Security expression engine
 * hasAuthority get values from Authentication.getAuthorities()
 * JwtAuthenticationConverter builds hasAuthority.
 * hasAuthority is not directly read from Jwt Parameter. But it is based on Jwt claims only after conversion.
 *
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class BookmarkService {
    private final BookmarkRepository repo;

    BookmarkService(BookmarkRepository repo) {
        this.repo = repo;
    }

    /*
    The findAll() method will load all the records in the table and this may lead to OutOfMemoryExceptions
    if there are millions of records. If the table is ever-growing with new data, it is always advised
    to use Pagination.
     */
    /*public List<Bookmark> findAll() {
        return repo.findAll();
    }*/


    /**
     * Find bookmarks with pagination.
     * RBAC: requires BOOKMARK_READ authority.
     * PBAC: JR/TL/Admin can read all; ROLE_USER can only read own bookmarks.
     */
    @Cacheable(value = "bookmarks", key = "#query.pageNo() + '-' + #query.pageSize()")
    @PreAuthorize("hasAuthority('BOOKMARK_READ')")
    public PagedResult<BookmarkDTO> findBookmarks(FindBookmarksQuery query, Jwt jwt) {
        log.info("BookmarkService findBookmarks() API Request: Find Bookmarks query={}, jwt={}", query, jwt);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        //from user POV, page number starts from 1, but for Spring Data JPA page number starts from 0.
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);

        // Use repository projection directly (DTOs)
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);

        // PBAC: ROLE_USER can only see own bookmarks
        String username = jwt.getClaims().get("username").toString();
        String roles = jwt.getClaims().get("roles").toString();

        // Extract authorities claim as a List
        List <String> authorities = jwt.getClaimAsStringList("authorities");

        /**
         * IN FILTER BLOCK :
         * This is a PBAC (Policy-Based Access Control) rule: USER role is constrained, others are not.
         * If the role is not USER → the left side is true, so the whole condition is true. → Non-USER roles (ADMIN, TL, JR, etc.) see all bookmarks.
         * If the role is USER → left side is false.
         *  1. → Then the condition depends on the right side: bookmarkDto.createdBy().equals(username).
         *  2. → USERs only see bookmarks they created.
         *
         *  Result:
         *  1. ROLE_USER → restricted to own bookmarks.
         *  2. Any other role → unrestricted, sees all bookmarks.
         */
        List<BookmarkDTO> bookmarkDtoList = page.getContent().stream()
                .filter(bookmarkDto ->
                        !(roles.equals("ROLE_USER"))
                                || bookmarkDto.createdBy().equals(username)
                )
                //.filter(bookmarkDto -> roles.equals("ROLE_USER") ? bookmarkDto.createdBy().equals(username) : true )
                .toList();

        log.info("Bookmarks List found : {}",bookmarkDtoList.isEmpty());
        return new PagedResult<BookmarkDTO>(
                bookmarkDtoList,
                page.getTotalElements(),
                page.getNumber() + 1, // for user page number starts from 1
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }


    public BookmarkDTO create(BookmarkDTO bookmark) {
        return new BookmarkDTO(0L,null,null,null,null,null);
    }

    /**
     * Create bookmark.
     * Owner set to logged-in user.
     */
    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority('BOOKMARK_WRITE')")
    public BookmarkDTO create(CreateBookmarkCommand cmd, Jwt jwt) {
        log.info("BookmarkService create() API Request: Persisting Bookmark cmd={}, jwt={}", cmd, jwt);
        String username = jwt.getClaimAsString("username");

        Bookmark bookmark = new Bookmark();

        bookmark.setCreatedAt(Instant.now());
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        bookmark.setCreatedBy(username);
        Bookmark savedBookmark = repo.save(bookmark);

        logAuditTrail("CREATE", username, savedBookmark.getId());

        // bookmark.setCreatedAt(Instant.now());
        log.info("Bookmark Persisted Successfully");
        return BookmarkDTO.from(savedBookmark);
    }

    /**
     * Update bookmark.
     * PBAC: only owner OR TL/Admin can update.
     */
    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority('BOOKMARK_UPDATE')")
    public BookmarkDTO update(UpdateBookmarkCommand cmd, Jwt jwt) {
        log.info("BookmarkService update() API Request: Update Bookmark cmd={}, jwt={}", cmd, jwt);
        Bookmark bookmark = repo.findById(cmd.id())
                .orElseThrow(() -> BookmarkNotFoundException.of(cmd.id()));

        String userRole = jwt.getClaimAsString("roles");
        String username = jwt.getClaimAsString("username");

        /**
         *  EVEN THOUGH LOGICAL AND HAS HIGHER PRECEDENCE THAN LOGICAL OR
         */
        if ( !(bookmark.getCreatedBy().equals(username)
                || userRole.equals("ROLE_TL")
                || userRole.equals("ROLE_ADMIN"))) {
            log.warn("UnAuthorized Bookmark Updating failed");
            throw new AccessDeniedException("Not authorized to update this bookmark :" + cmd.id());
        }

        /*if(userRole.equals("ROLE_USER") && !bookmark.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not authorized to update this bookmark");
        }
        if(userRole.equals("ROLE_JR")) {
            throw new AccessDeniedException("Not authorized to update this bookmark");
        }*/

        /*switch (userRole) {
            case "ROLE_USER" -> {
                if (!bookmark.getCreatedBy().equals(username)) {
                    throw new AccessDeniedException("Not authorized to update this bookmark");
                }
            }
            case "ROLE_JR" -> throw new AccessDeniedException("Not authorized to update this bookmark");
            // ROLE_ADMIN and ROLE_TL fall through → allowed
        }*/



        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        bookmark.setUpdatedBy(username);
        bookmark.setUpdatedAt(Instant.now());
        Bookmark updatedBookmark = repo.save(bookmark);

        logAuditTrail("UPDATED :", username, bookmark.getId());
        log.info("Bookmark Updated Successfully");
        return BookmarkDTO.from(updatedBookmark);
    }

   /* public Optional<BookmarkDTO> findById(Long id) {
        return repo.findBookmarkById(id);
    }*/

    /**
     * Find bookmark by ID.
     * PBAC: ROLE_USER can only read own bookmarks.
     */
    @Cacheable(value = "bookmarks", key = "#id")
    @PreAuthorize("hasAuthority('BOOKMARK_READ')")
    public BookmarkDTO findById(Long id, Jwt jwt) {
        log.info("BookmarkService findById() API Request: Find Bookmark id={}, jwt={}", id, jwt);
        //return repo.findBookmarkById(id);
        BookmarkDTO bookmarkDTO = repo.findBookmarkById(id)
                .orElseThrow(() -> BookmarkNotFoundException.of(id));
        String userRole = jwt.getClaimAsString("roles");
        String username = jwt.getClaimAsString("username");
        List<String> authoritiesList = jwt.getClaimAsStringList("authorities");
        log.debug("BookmarkService findById : userRole: {}, username : {}, authoritiesList : {}",userRole,username,authoritiesList);
        if ( userRole.equals("ROLE_USER") && !bookmarkDTO.createdBy().equals(username)) {
            log.warn("UnAuthorized Bookmark findById ");
            throw new AccessDeniedException("Not authorized to View this bookmark :" + id);
        }
        log.debug("BookmarkService findById : BookmarkDTO Response : {}",bookmarkDTO);

        /**
         * 6️⃣ How To Verify What Authorities Spring Sees Inside your method, temporarily log: If it prints empty → mapping problem.
         *   Spring will NOT map "authorities" → GrantedAuthority objects.
         *   If not mapped: >> Authentication.getAuthorities() = []
         *   Then: hasAuthority('BOOKMARK_READ') = FALSE , Even though it exists in the token.
         *   5️⃣ What @PreAuthorize Actually Checks > When you write: @PreAuthorize("hasAuthority('BOOKMARK_READ')")
         *   authentication.getAuthorities().contains(new SimpleGrantedAuthority("BOOKMARK_READ"))
         *   then : If not found → AccessDeniedException (403).
         *   7️⃣ Important: Our Manual JWT Check Is Separate
         *   String userRole = jwt.getClaimAsString("roles");
         *   List<String> authoritiesList = jwt.getClaimAsStringList("authorities");
         *   This is manual claim reading. It has NOTHING to do with @PreAuthorize.
         *   @PreAuthorize works entirely from: SecurityContext Authentication object
         *   8️⃣ Correct Architecture Model :
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authorities from SecurityContext: {}",authentication.getAuthorities());

        return bookmarkDTO;
    }

    /**
     * Delete bookmark.
     * PBAC: only owner OR Admin can delete.
     */
    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority('BOOKMARK_DELETE')")
    public String delete(Long postId, Jwt jwt) {
        log.info("BookmarkService delete() API Request: Delete Bookmark postId={}, jwt={}", postId, jwt);
        Bookmark bookmark = repo.findById(postId)
                .orElseThrow(()-> BookmarkNotFoundException.of(postId));

        String userRole = jwt.getClaimAsString("roles");
        String username = jwt.getClaimAsString("username");

        /**
         * 🎯 Meaning
         * If the user is the creator → first part is false → condition fails → allowed.
         * If the user is ADMIN → second part is false → condition fails → allowed.
         * If the user is neither the creator nor ADMIN → both parts true → condition passes → denied.
         */
        if ( !bookmark.getCreatedBy().equals(username) && !userRole.equals("ROLE_ADMIN")) {
            log.warn("Unauthorized Bookmark deletion attempt");
            throw new AccessDeniedException("Not authorized to delete this bookmark :" + postId);
        }

        repo.delete(bookmark);

        logAuditTrail("DELETE", username, bookmark.getId());

        log.info("Bookmark deleted successfully");

        return "Bookmark with id=" + bookmark.getId() + " deleted successfully";
    }

    private void logAuditTrail(String actionMsg, String user, Long bookmarkId) {
        // production: persist audit trail in DB or external system
        log.info("AUDIT: {} by {} on bookmark {} at {}", actionMsg, user, bookmarkId, Instant.now());
    }
}