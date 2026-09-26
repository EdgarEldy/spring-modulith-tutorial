package com.edgareldy.springmodulithtutorial.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Unit tests of {@link JwtService}: issuing, validating, and refusing expired, revoked or forged tokens.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!";

    private final BlacklistedTokenRepository blacklist = mock(BlacklistedTokenRepository.class);
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(properties(SECRET, Duration.ofHours(1)), blacklist, Clock.systemUTC());
        Role role = mock(Role.class);
        when(role.getRoleName()).thenReturn(Role.USER);
        user = new User("Ada", "Lovelace", "ada@example.com", "{noop}secret");
        user.setId(42L);
        user.addRole(role);
    }

    @Test
    void _01_ShouldDecodeTheClaims_WhenTheTokenWasJustIssued() {
        Jwt issued = jwtService.issue(user);

        Jwt decoded = jwtService.decode(issued.getTokenValue());

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getId()).isNotBlank();
        assertThat(decoded.getClaimAsStringList(JwtService.ROLES_CLAIM)).containsExactly(Role.USER);
        assertThat(decoded.getClaimAsString(JwtService.EMAIL_CLAIM)).isEqualTo("ada@example.com");
        assertThat(decoded.getExpiresAt()).isEqualTo(issued.getExpiresAt());
    }

    @Test
    void _02_ShouldRejectTheToken_WhenItHasExpired() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        JwtService issuerInThePast = new JwtService(properties(SECRET, Duration.ofHours(1)), blacklist, twoHoursAgo);
        String expired = issuerInThePast.issue(user).getTokenValue();

        assertThatThrownBy(() -> jwtService.decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void _03_ShouldRejectTheToken_WhenItsJtiIsBlacklisted() {
        Jwt issued = jwtService.issue(user);
        when(blacklist.existsByJti(issued.getId())).thenReturn(true);

        assertThatThrownBy(() -> jwtService.decode(issued.getTokenValue()))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void _04_ShouldRejectTheToken_WhenItWasSignedWithAnotherKey() {
        JwtService otherKey = new JwtService(
                properties("another-secret-with-at-least-thirty-two-bytes", Duration.ofHours(1)), blacklist,
                Clock.systemUTC());
        when(blacklist.existsByJti(anyString())).thenReturn(false);
        String forged = otherKey.issue(user).getTokenValue();

        assertThatThrownBy(() -> jwtService.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void _05_ShouldRefuseToStart_WhenTheSecretIsShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService(properties("too-short", Duration.ofHours(1)), blacklist))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AuthProperties properties(String secret, Duration ttl) {
        return new AuthProperties("http://localhost:8080", Duration.ofHours(24), Duration.ofMinutes(15),
                new AuthProperties.Jwt(secret, "test-issuer", ttl), new AuthProperties.Admin(null, null));
    }
}
