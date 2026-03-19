package com.learncicd.authservice.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Role vs Authorities duplication
 * This class is used to map roles to authorities
 * Best practice: store only the role in DB, derive authorities dynamically via RoleAuthorityMapper.
 */

@Component
@Slf4j
public class RoleAuthorityMapper {
    private RoleAuthorityMapper(){}
    public static Set<UserAuthority> getAuthorities(UserRole role) {
        log.info("RoleAuthorityMapper getAuthorities role={}", role);
        switch (role) {
            case ROLE_ADMIN -> {
                log.info("RoleAuthorityMapper  ROLE_ADMIN case : role={}", role);
                return Set.of(UserAuthority.BOOKMARK_READ,
                        UserAuthority.BOOKMARK_UPDATE,
                        UserAuthority.BOOKMARK_DELETE);
            }
            case ROLE_TL -> {
                log.info("RoleAuthorityMapper  ROLE_TL case : role={}", role);
                return Set.of(UserAuthority.BOOKMARK_READ,
                        UserAuthority.BOOKMARK_UPDATE);
            }
            case ROLE_JR -> {
                log.info("RoleAuthorityMapper  ROLE_JR case : role={}", role);
                return Set.of(UserAuthority.BOOKMARK_READ);
            }
            case ROLE_USER -> {
                log.info("RoleAuthorityMapper  ROLE_USER case : role={}", role);
                return Set.of(UserAuthority.BOOKMARK_READ,
                        UserAuthority.BOOKMARK_WRITE,
                        UserAuthority.BOOKMARK_UPDATE,
                        UserAuthority.BOOKMARK_DELETE);
            }
            default -> {
                log.info("RoleAuthorityMapper  default case : role={}", role);
                return Set.of();
            }
        }
    }
}

