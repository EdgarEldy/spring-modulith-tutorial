package com.edgareldy.springmodulithtutorial.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository tests of the auth module against the real PostgreSQL schema created by Flyway.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as the other integration tests, so the cached context and its container are reused.
// @Transactional rolls every test back, leaving the shared database clean for the HTTP tests.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthRepositoriesTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void _01_ShouldFindTheSeededRoles_WhenLookingThemUpByName() {
        assertThat(roleRepository.findByRoleName(Role.ADMIN)).isPresent();
        assertThat(roleRepository.findByRoleName(Role.USER)).isPresent();
    }

    @Test
    void _02_ShouldPersistTheUserWithItsRoles_WhenSavedThroughTheJoinTable() {
        User user = newUser();
        entityManager.flush();
        entityManager.clear();

        User found = userRepository.findByEmail(user.getEmail()).orElseThrow();

        assertThat(found.getRoles()).extracting(Role::getRoleName).containsExactly(Role.USER);
        assertThat(found.isEnabled()).isFalse();
        assertThat(userRepository.existsByEmail(user.getEmail())).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void _03_ShouldFindTheActivationToken_WhenLookingItUpByValue() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        activationTokenRepository.save(new ActivationToken(newUser(), "activation-value", now, now.plusSeconds(60)));
        entityManager.flush();
        entityManager.clear();

        ActivationToken found = activationTokenRepository.findByToken("activation-value").orElseThrow();

        assertThat(found.getExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(found.getValidatedAt()).isNull();
    }

    @Test
    void _04_ShouldReportTheJtiAsRevoked_WhenTheTokenIsBlacklisted() {
        Instant now = Instant.now();
        blacklistedTokenRepository.save(
                new BlacklistedToken(newUser().getId(), "a.b.c", "jti-revoked", now, now, now.plusSeconds(60)));
        entityManager.flush();

        assertThat(blacklistedTokenRepository.existsByJti("jti-revoked")).isTrue();
        assertThat(blacklistedTokenRepository.existsByJti("jti-unknown")).isFalse();
    }

    @Test
    void _05_ShouldDeleteThePendingResetTokens_WhenDeletingByUser() {
        User user = newUser();
        passwordResetTokenRepository.save(new PasswordResetToken(user, "reset-1", Instant.now().plusSeconds(60)));
        passwordResetTokenRepository.save(new PasswordResetToken(user, "reset-2", Instant.now().plusSeconds(60)));
        entityManager.flush();

        passwordResetTokenRepository.deleteByUser(user);
        entityManager.flush();

        assertThat(passwordResetTokenRepository.findByToken("reset-1")).isEmpty();
        assertThat(passwordResetTokenRepository.findByToken("reset-2")).isEmpty();
    }

    private User newUser() {
        User user = new User("Ada", "Lovelace", UUID.randomUUID() + "@example.com", "{noop}password123");
        user.addRole(roleRepository.findByRoleName(Role.USER).orElseThrow());
        return userRepository.save(user);
    }
}
