package com.edgareldy.springmodulithtutorial.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests of {@link AdminBootstrap}: an ADMIN account only when both variables are set and it does not exist yet.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
class AdminBootstrapTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void _01_ShouldDoNothing_WhenTheVariablesAreAbsent() {
        bootstrap(new AuthProperties.Admin("", "")).run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void _02_ShouldDoNothing_WhenOnlyTheEmailIsSet() {
        bootstrap(new AuthProperties.Admin("admin@example.com", null)).run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void _03_ShouldCreateAnEnabledAdmin_WhenBothVariablesAreSetAndTheAccountIsMissing() {
        Role adminRole = mock(Role.class);
        when(adminRole.getRoleName()).thenReturn(Role.ADMIN);
        when(roleRepository.findByRoleName(Role.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("S3cret-admin")).thenReturn("hashed");

        bootstrap(new AuthProperties.Admin("Admin@Example.com", "S3cret-admin")).run(new DefaultApplicationArguments());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(saved.getValue().getRoles()).containsExactly(adminRole);
    }

    @Test
    void _04_ShouldLeaveTheAccountUntouched_WhenItAlreadyExists() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        bootstrap(new AuthProperties.Admin("admin@example.com", "S3cret-admin")).run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    private AdminBootstrap bootstrap(AuthProperties.Admin admin) {
        AuthProperties properties = new AuthProperties("http://localhost:8080", Duration.ofHours(24),
                Duration.ofMinutes(15), new AuthProperties.Jwt("unused", "test-issuer", Duration.ofHours(1)), admin);
        return new AdminBootstrap(properties, userRepository, roleRepository, passwordEncoder);
    }
}
