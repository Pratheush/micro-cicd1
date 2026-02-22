package com.learncicd.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;


/**
 *  ⚠️ Optional: remove authorities from DB if you derive them dynamically since it will be redundant.
 *  persisting authorities in the DB and deriving them from the role. That’s redundant.
 *   store only the role in DB, derive authorities dynamically via RoleAuthorityMapper.
 */
@Entity
@Table(name="auth_usr")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;

    // Each user gets exactly one role. This stores the role directly in the auth_usr table as a string.
    @Enumerated(EnumType.STRING)
    private UserRole roles;

    /*@ElementCollection(targetClass = UserAuthority.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<UserAuthority> authorities = new HashSet<>();*/
}
