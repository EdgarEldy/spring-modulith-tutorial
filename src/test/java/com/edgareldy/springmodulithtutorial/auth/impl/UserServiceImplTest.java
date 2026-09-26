package com.edgareldy.springmodulithtutorial.auth.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.auth.ActivationToken;
import com.edgareldy.springmodulithtutorial.auth.ActivationTokenRepository;
import com.edgareldy.springmodulithtutorial.auth.AuthProperties;
import com.edgareldy.springmodulithtutorial.auth.BlacklistedToken;
import com.edgareldy.springmodulithtutorial.auth.BlacklistedTokenRepository;
import com.edgareldy.springmodulithtutorial.auth.JwtService;
import com.edgareldy.springmodulithtutorial.auth.LoginResponse;
import com.edgareldy.springmodulithtutorial.auth.PasswordResetToken;
import com.edgareldy.springmodulithtutorial.auth.PasswordResetTokenRepository;
import com.edgareldy.springmodulithtutorial.auth.Role;
import com.edgareldy.springmodulithtutorial.auth.RoleRepository;
import com.edgareldy.springmodulithtutorial.auth.User;
import com.edgareldy.springmodulithtutorial.auth.UserProfile;
import com.edgareldy.springmodulithtutorial.auth.UserRepository;
import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests of {@link UserServiceImpl} with mocked repositories, encoder and JWT service, on a fixed clock.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
class UserServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-26T10:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final ActivationTokenRepository activationTokenRepository = mock(ActivationTokenRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final BlacklistedTokenRepository blacklistedTokenRepository = mock(BlacklistedTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private UserServiceImpl service;
    private Role userRole;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties("http://localhost:8080", Duration.ofHours(24),
                Duration.ofMinutes(15), new AuthProperties.Jwt("unused", "test-issuer", Duration.ofHours(1)),
                new AuthProperties.Admin(null, null));
        service = new UserServiceImpl(userRepository, roleRepository, activationTokenRepository,
                passwordResetTokenRepository, blacklistedTokenRepository, passwordEncoder, jwtService, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        userRole = mock(Role.class);
        when(userRole.getRoleName()).thenReturn(Role.USER);
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hashed:" + invocation.getArgument(0));
        when(passwordEncoder.matches(any(), any()))
                .thenAnswer(invocation -> ("hashed:" + invocation.getArgument(0)).equals(invocation.getArgument(1)));
    }

    @Test
    void _01_ShouldCreateADisabledUserWithAnActivationToken_WhenRegisteringANewEmail() {
        when(roleRepository.findByRoleName(Role.USER)).thenReturn(Optional.of(userRole));

        UserProfile profile = service.register("Ada", "Lovelace", " Ada@Example.com ", "password123");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().isEnabled()).isFalse();
        assertThat(user.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(user.getValue().getPassword()).isEqualTo("hashed:password123");
        assertThat(profile.roles()).containsExactly(Role.USER);
        ArgumentCaptor<ActivationToken> token = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(token.capture());
        assertThat(token.getValue().getToken()).isNotBlank();
        assertThat(token.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void _02_ShouldRejectTheRegistration_WhenTheEmailIsAlreadyUsed() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Ada", "Lovelace", "ada@example.com", "password123"))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void _03_ShouldEnableTheUserAndConsumeTheToken_WhenActivatingWithAValidToken() {
        User user = user(false);
        ActivationToken token = new ActivationToken(user, "abc", NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        service.activate("abc");

        assertThat(user.isEnabled()).isTrue();
        assertThat(token.getValidatedAt()).isEqualTo(NOW);
    }

    @Test
    void _04_ShouldRejectTheActivation_WhenTheTokenHasExpired() {
        User user = user(false);
        when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(
                new ActivationToken(user, "abc", NOW.minusSeconds(120), NOW.minusSeconds(60))));

        assertThatThrownBy(() -> service.activate("abc")).isInstanceOf(BusinessRuleException.class);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void _05_ShouldRejectTheActivation_WhenTheTokenWasAlreadyUsed() {
        ActivationToken token = new ActivationToken(user(true), "abc", NOW.minusSeconds(60), NOW.plusSeconds(60));
        token.markValidated(NOW.minusSeconds(30));
        when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.activate("abc")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void _06_ShouldReturnABearerToken_WhenTheCredentialsAreValid() {
        User user = user(true);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        Jwt jwt = Jwt.withTokenValue("signed.jwt.value").header("alg", "HS256").subject("1")
                .issuedAt(NOW).expiresAt(NOW.plusSeconds(3600)).build();
        when(jwtService.issue(user)).thenReturn(jwt);

        LoginResponse response = service.login("ada@example.com", "password123");

        assertThat(response.accessToken()).isEqualTo("signed.jwt.value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void _07_ShouldAnswerTheSame401_WhenTheEmailIsUnknownOrThePasswordIsWrong() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> service.login("nobody@example.com", "password123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertUnauthorized(ex, "Invalid email or password"));
        assertThatThrownBy(() -> service.login("ada@example.com", "wrong-password"))
                .satisfies(ex -> assertUnauthorized(ex, "Invalid email or password"));
    }

    @Test
    void _08_ShouldRefuseTheLogin_WhenTheAccountIsNotActivated() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> service.login("ada@example.com", "password123"))
                .satisfies(ex -> assertUnauthorized(ex, "Account is not activated"));
    }

    @Test
    void _09_ShouldBlacklistTheJti_WhenLoggingOut() {
        Jwt jwt = Jwt.withTokenValue("signed.jwt.value").header("alg", "HS256").subject("7").jti("jti-1")
                .issuedAt(NOW.minusSeconds(60)).expiresAt(NOW.plusSeconds(3540)).build();

        service.logout(jwt);

        ArgumentCaptor<BlacklistedToken> saved = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getJti()).isEqualTo("jti-1");
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getBlacklistedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(3540));
    }

    @Test
    void _10_ShouldReplaceThePendingResetTokens_WhenTheEmailIsRegistered() {
        User user = user(true);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        service.forgotPassword("ada@example.com");

        verify(passwordResetTokenRepository).deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getExpiryDate()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(saved.getValue().getType()).isEqualTo(PasswordResetToken.TYPE_PASSWORD_RESET);
    }

    @Test
    void _11_ShouldReturnWithoutIssuingAToken_WhenTheEmailIsUnknown() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("nobody@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void _12_ShouldChangeThePasswordAndDeleteTheToken_WhenResettingWithAValidToken() {
        User user = user(true);
        PasswordResetToken token = new PasswordResetToken(user, "reset", NOW.plusSeconds(60));
        when(passwordResetTokenRepository.findByToken("reset")).thenReturn(Optional.of(token));

        service.resetPassword("reset", "new-password");

        assertThat(user.getPassword()).isEqualTo("hashed:new-password");
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void _13_ShouldRejectAndDeleteTheToken_WhenItHasExpired() {
        User user = user(true);
        PasswordResetToken token = new PasswordResetToken(user, "reset", NOW.minusSeconds(1));
        when(passwordResetTokenRepository.findByToken("reset")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("reset", "new-password"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(user.getPassword()).isEqualTo("hashed:password123");
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void _14_ShouldRejectTheReset_WhenTheTokenIsUnknown() {
        when(passwordResetTokenRepository.findByToken("reset")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("reset", "new-password"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void _15_ShouldThrowNotFound_WhenTheProfileOfAMissingUserIsRequested() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.me(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(boolean enabled) {
        User user = new User("Ada", "Lovelace", "ada@example.com", "hashed:password123");
        user.setEnabled(enabled);
        user.addRole(userRole);
        return user;
    }

    private static void assertUnauthorized(Throwable ex, String reason) {
        ResponseStatusException status = (ResponseStatusException) ex;
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status.getReason()).isEqualTo(reason);
    }
}
