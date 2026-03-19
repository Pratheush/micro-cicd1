package com.learncicd.authservice.service;

import com.learncicd.authservice.model.RoleAuthorityMapper;
import com.learncicd.authservice.model.User;
import com.learncicd.authservice.model.UserAuthority;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * purpose of this class : In spring security when username and password is verified then it returns object i.e. UserDetails which is spring-security but we need our own UserDetails
 *
 * JPA entities should implement Serializable. You already did that, which is correct.
 *
 * The warning about user in MyUserDetails comes because UserDetails is serializable, but your User field isn’t marked transient
 * or Serializable. Since your User entity already implements Serializable, you can safely ignore or suppress this warning.
 *
 */

@Slf4j
public class MyUserDetails implements UserDetails {

    private final User user;

    public MyUserDetails(User user){
        this.user = user;
        log.debug("MyUserDetails constructor user={}", user.getUsername());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.debug("MyUserDetails getAuthorities() user={}", user.getUsername());
        /*List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        // ✅ Map role to authorities dynamically
        Set<UserAuthority> mappedAuthorities = RoleAuthorityMapper.getAuthorities(user.getRoles());

        // ✅ Convert to GrantedAuthority list
        mappedAuthorities.stream()
                .map(userAuthority -> new SimpleGrantedAuthority(userAuthority.name()))
                .forEach(grantedAuthorities::add);

        // ✅ Add role itself as a GrantedAuthority (Spring expects ROLE_ prefix)
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getRoles().name()));

        //return Arrays.stream(user.getRoles().split(",")).map(SimpleGrantedAuthority::new).toList();
       return grantedAuthorities;*/

        // ✅ Map role to authorities dynamically
        Set<UserAuthority> mappedAuthorities = RoleAuthorityMapper.getAuthorities(user.getRoles());
        log.debug("MyUserDetails getAuthorities mappedAuthorities={}", mappedAuthorities);
        // ✅ Convert to GrantedAuthority list
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>( mappedAuthorities
                .stream()
                .map(auth -> new SimpleGrantedAuthority(auth.name()))
                .toList());
        log.debug("MyUserDetails getAuthorities grantedAuthorities={}", grantedAuthorities);
        // ✅ Add role itself as a GrantedAuthority (Spring expects ROLE_ prefix)
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getRoles().name()));
        log.debug("MyUserDetails getAuthorities grantedAuthorities={}", grantedAuthorities);
        return grantedAuthorities;
    }

    @Override
    @Nonnull
    public String getPassword() {
        log.debug("MyUserDetails getPassword() user={}", user.getUsername());
        return user.getPassword();
    }

    @Override
    @Nonnull
    public String getUsername() {
        log.debug("MyUserDetails getUsername() user={}", user.getUsername());
        return user.getUsername();
    }

    // ✅ Explicitly return true unless you implement account state logic
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
