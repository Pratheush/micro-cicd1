package com.learncicd.authservice.model;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Role vs Authorities duplication
 * This class is used to map roles to authorities
 * Best practice: store only the role in DB, derive authorities dynamically via RoleAuthorityMapper.
 */

@Component
public class RoleAuthorityMapper {
    private RoleAuthorityMapper(){}
    public static Set<UserAuthority> getAuthorities(UserRole role) {
        switch (role) {
            case ROLE_ADMIN -> {
                return Set.of(UserAuthority.BOOKMARK_READ,
                        //UserAuthority.BOOKMARK_WRITE,
                        UserAuthority.BOOKMARK_UPDATE,
                        UserAuthority.BOOKMARK_DELETE);
            }
            case ROLE_TL -> {
                return Set.of(UserAuthority.BOOKMARK_READ,
                        //UserAuthority.BOOKMARK_WRITE,
                        UserAuthority.BOOKMARK_UPDATE);
            }
            case ROLE_JR -> {
                return Set.of(UserAuthority.BOOKMARK_READ);
            }
            case ROLE_USER -> {
                return Set.of(UserAuthority.BOOKMARK_READ,
                        UserAuthority.BOOKMARK_WRITE,
                        UserAuthority.BOOKMARK_UPDATE,
                        UserAuthority.BOOKMARK_DELETE);
            }
            default -> {
                return Set.of();
            }
        }
    }
}

