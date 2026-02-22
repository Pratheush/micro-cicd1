package com.learncicd.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;

@Component
@Slf4j
public class JwtUtil {

    public static final String SECRET_KEY = "UHJhdGhldXNoUkFKUkAyMjc0MTIjQFNHSDE5ODlNaWNyb0NJQ0RPYnNlcnZhYmlsaXR5UGVyZm9ybWFuY2VBbmRBTExLaW5kc09GRi1UZXN0aW5n";



    private Key getKey() {
        byte[] bytes = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(bytes);
    }


    public void validateToken(String token){
        log.info("Api-Gateway JwtUtil : validateToken");
        Jwts.parser()
                .verifyWith((SecretKey) getKey())
                .build()
                .parseSignedClaims(token);
    }
}
