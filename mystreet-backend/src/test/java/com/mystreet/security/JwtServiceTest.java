package com.mystreet.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mystreet.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class JwtServiceTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-0123456789abcdef";
    private static final String OTHER_SECRET = "another-secret-for-unit-tests-only-9876543210fedcba";

    private User admin() {
        User u = new User();
        u.setEmail("admin@test.com");
        u.setFullName("Admin Test");
        u.setAdmin(true);
        return u;
    }

    @Test
    void generatedToken_containsExpectedClaims() {
        JwtService service = new JwtService(SECRET, 60);

        Claims claims = service.parse(service.generateToken(admin()));

        assertThat(claims.getSubject()).isEqualTo("admin@test.com");
        assertThat(claims.get("name", String.class)).isEqualTo("Admin Test");
        assertThat(claims.get("isAdmin", Boolean.class)).isTrue();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void expiredToken_isRejected() {
        JwtService service = new JwtService(SECRET, -1);
        String token = service.generateToken(admin());

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenWithForgedPayload_isRejected() {
    JwtService service = new JwtService(SECRET, 60);
    User regularUser = new User();
    regularUser.setEmail("ana@test.com");
    regularUser.setFullName("Ana");
    regularUser.setAdmin(false);

    String[] parts = service.generateToken(regularUser).split("\\.");
    String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            "{\"sub\":\"ana@test.com\",\"isAdmin\":true}".getBytes(StandardCharsets.UTF_8));
    String forgedToken = parts[0] + "." + forgedPayload + "." + parts[2];

    assertThatThrownBy(() -> service.parse(forgedToken)).isInstanceOf(JwtException.class);
}

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String foreignToken = new JwtService(OTHER_SECRET, 60).generateToken(admin());
        JwtService service = new JwtService(SECRET, 60);

        assertThatThrownBy(() -> service.parse(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecret_isRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 60)).isInstanceOf(WeakKeyException.class);
    }
}
