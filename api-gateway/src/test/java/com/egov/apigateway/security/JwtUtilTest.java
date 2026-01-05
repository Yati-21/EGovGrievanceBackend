package com.egov.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET =
            "egov-secret-key-should-be-very-long-and-secure-256bit";

    @Test
    void validateAndGetClaims_success() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject("USER123")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .signWith(key)
                .compact();

        JwtUtil jwtUtil = new JwtUtil(SECRET);
        Claims claims = jwtUtil.validateAndGetClaims(token);

        assertEquals("USER123", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void constructor_blankSecret_throwsException() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil(""));
    }
}
