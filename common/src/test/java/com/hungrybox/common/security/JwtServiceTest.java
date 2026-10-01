package com.hungrybox.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-definitely-long-enough-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(5));

    @Test
    void issuedTokenRoundTrips() {
        String token = jwtService.issue(42L, AuthenticatedUser.ROLE_ADMIN);

        AuthenticatedUser user = jwtService.verify(token);

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService("a-completely-different-secret-also-long-enough-987654", Duration.ofMinutes(5));
        String token = other.issue(1L, AuthenticatedUser.ROLE_CUSTOMER);

        assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expired = new JwtService(SECRET, Duration.ofMinutes(-1));
        String token = expired.issue(1L, AuthenticatedUser.ROLE_CUSTOMER);

        assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.verify("not-a-jwt")).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new JwtService("too-short", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicPathsHonourHttpMethod() {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService,
                List.of("GET /api/restaurants/**", "POST /api/users/login"));

        assertThat(filter.isPublic("GET", "/api/restaurants/3")).isTrue();
        assertThat(filter.isPublic("POST", "/api/restaurants")).isFalse();
        assertThat(filter.isPublic("POST", "/api/users/login")).isTrue();
        assertThat(filter.isPublic("GET", "/actuator/health/readiness")).isTrue();
        assertThat(filter.isPublic("GET", "/api/orders")).isFalse();
    }
}
