package com.egov.user.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtUtilTest {

    JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret", 100000);

    @Test
    void generate_and_validate_token() {
        String token = jwtUtil.generateToken("user1", "ADMIN");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user1", jwtUtil.getUserIdFromToken(token));
        assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void invalid_token() {
        assertFalse(jwtUtil.validateToken("invalid.token"));
    }
}
