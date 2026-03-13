package com.algorecall.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // A 256-bit Base64-encoded secret for testing
    private static final String TEST_SECRET = "dGhpc2lzYXRlc3RzZWNyZXRrZXl0aGF0aXNsb25nZW5vdWdoZm9yaG1hYzI1Ng==";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", EXPIRATION_MS);
    }

    private UserDetails createUserDetails(String username) {
        return new org.springframework.security.core.userdetails.User(
                username, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void generateToken_createsValidToken() {
        UserDetails userDetails = createUserDetails("alice");

        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        UserDetails userDetails = createUserDetails("alice");
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        UserDetails alice = createUserDetails("alice");
        UserDetails bob = createUserDetails("bob");

        String token = jwtUtil.generateToken(alice);

        assertThat(jwtUtil.isTokenValid(token, bob)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Create jwt util with 0ms expiration
        JwtUtil shortLivedUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedUtil, "jwtExpirationMs", 0L);

        UserDetails userDetails = createUserDetails("alice");
        String token = shortLivedUtil.generateToken(userDetails);

        // Token is already expired
        assertThatThrownBy(() -> shortLivedUtil.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractUsername_validToken() {
        UserDetails userDetails = createUserDetails("bob");
        String token = jwtUtil.generateToken(userDetails);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("bob");
    }

    @Test
    void extractUsername_invalidToken_throws() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_withExtraClaims() {
        UserDetails userDetails = createUserDetails("alice");
        java.util.Map<String, Object> claims = java.util.Map.of("role", "ADMIN");

        String token = jwtUtil.generateToken(claims, userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }
}
