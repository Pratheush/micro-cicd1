package com.learncicd.authservice.util;

import com.learncicd.authservice.model.RoleAuthorityMapper;
import com.learncicd.authservice.model.User;
import com.learncicd.authservice.repository.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *  Auth-service enriches JWT at login:
 *  - username
 *  - email
 *  - roles
 *  - issuer
 *  - expiration
 *  - claims
 *  downstream services validate JWT & extract claims
 *  User-service extracts claims from JWT (via Spring Security or a utility class).
 */
@Component
public class JwtUtil {

    //private final String SECRET_KEY= "ASHHDFHSOIUEUBDIFBUIEWGFVSDVFIWWEE487536DGKFHGHDSGFHKSDGFUEFUEVCUKEUFUDVCVDHSVHSDVHF";

    // PratheushRAJR@227412#@SGH1989MicroCICDObservabilityPerformanceAndALLKindsOFF-Testing : Base64 encoded
    private final String SECRET_KEY= "UHJhdGhldXNoUkFKUkAyMjc0MTIjQFNHSDE5ODlNaWNyb0NJQ0RPYnNlcnZhYmlsaXR5UGVyZm9ybWFuY2VBbmRBTExLaW5kc09GRi1UZXN0aW5n";

    private final UserRepo userRepo;

    public JwtUtil(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public String generateToken(String username){

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET_KEY)
        );

        // 1000: 1 second & 60 second & 5 minutes
        // using JWT, embed email,username,roles and authorities in claims.
        // This makes authorization checks lightweight and distributed.
        Map<String,Object> claims = new HashMap<>();

        User userFound = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        claims.put("roles",userFound.getRoles());
        claims.put("authorities", RoleAuthorityMapper.getAuthorities(userFound.getRoles()));
        claims.put("email",userFound.getEmail());
        claims.put("username",userFound.getUsername());

        return Jwts.builder()
                .subject(username)
                .issuer("raj@outlook.com")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000*60*5))
                .claims(claims)
                //.signWith(getKey())  // ← THIS LINE decides algorithm : If key is Keys.secretKeyFor(SignatureAlgorithm.HS512) then decoder must also expect HS512. SO FORCE THE SAME ALGORITHM EVERYWHERE IN EVERY MICRO-SERVICES
                .signWith(key, Jwts.SIG.HS512)  // HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every microservices
                .compact();
    }

    // SECRET_KEY.getBytes(StandardCharsets.UTF_8) this will also return byte[] : if secret-key is plain-text then use this
    // if secret-key is in base64 then use Base64.getDecoder().decode(SECRET_KEY)
    private Key getKey(){
        byte[] decodeKey = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(decodeKey);

    }

    public Date expirationDate(String token){
        return getClaims(token).getExpiration();
    }

    // type-casting into SecretKey since SecretKey is extending Key
    private Claims getClaims(String token){
        return Jwts.parser().verifyWith((SecretKey) getKey())
                .build().parseSignedClaims(token)
                .getPayload();
    }

}
