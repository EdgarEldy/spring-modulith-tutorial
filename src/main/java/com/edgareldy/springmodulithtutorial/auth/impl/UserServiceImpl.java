package com.edgareldy.springmodulithtutorial.auth.impl;

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
import com.edgareldy.springmodulithtutorial.auth.UserService;
import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Default {@link UserService}: persists accounts and tokens, hashes passwords and issues JWTs.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Service
class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final Clock clock;

    /**
     * Constructor used by Spring, on the system UTC clock.
     *
     * @param userRepository               accounts
     * @param roleRepository               seeded roles
     * @param activationTokenRepository    activation tokens
     * @param passwordResetTokenRepository password reset tokens
     * @param blacklistedTokenRepository   revoked JWTs
     * @param passwordEncoder              password hashing
     * @param jwtService                   token issuing
     * @param properties                   module settings (base URL, token lifetimes)
     */
    @Autowired
    UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                    ActivationTokenRepository activationTokenRepository,
                    PasswordResetTokenRepository passwordResetTokenRepository,
                    BlacklistedTokenRepository blacklistedTokenRepository, PasswordEncoder passwordEncoder,
                    JwtService jwtService, AuthProperties properties) {
        this(userRepository, roleRepository, activationTokenRepository, passwordResetTokenRepository,
                blacklistedTokenRepository, passwordEncoder, jwtService, properties, Clock.systemUTC());
    }

    UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                    ActivationTokenRepository activationTokenRepository,
                    PasswordResetTokenRepository passwordResetTokenRepository,
                    BlacklistedTokenRepository blacklistedTokenRepository, PasswordEncoder passwordEncoder,
                    JwtService jwtService, AuthProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
    }

    // @Transactional wraps the account, its role link and its activation token in one database
    // transaction: if any insert fails, none of them is kept, so no account is left without a token.
    @Override
    @Transactional
    public UserProfile register(String firstName, String lastName, String email, String rawPassword) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException("An account already exists for this email");
        }
        Role userRole = roleRepository.findByRoleName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Role " + Role.USER + " is not seeded"));
        User user = new User(firstName, lastName, normalizedEmail, passwordEncoder.encode(rawPassword));
        user.addRole(userRole);
        userRepository.save(user);

        Instant now = clock.instant();
        String token = randomToken();
        activationTokenRepository.save(
                new ActivationToken(user, token, now, now.plus(properties.activationTokenTtl())));
        // Stands in for the activation email: the tutorial has no mail server, so the link is logged.
        log.info("Activation link for {}: {}/api/v1/auth/activate-account?token={}",
                normalizedEmail, properties.baseUrl(), token);
        return UserProfile.from(user);
    }

    @Override
    @Transactional
    public void activate(String token) {
        Instant now = clock.instant();
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired activation token"));
        activationToken.markValidated(now);
        activationToken.getUser().setEnabled(true);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        // Same message for an unknown email and a wrong password, so the answer never reveals
        // which accounts exist. ResponseStatusException implements ErrorResponse: the
        // GlobalExceptionHandler renders it as a 401 ApiResponse with this reason.
        User user = userRepository.findByEmail(normalize(email))
                .filter(candidate -> passwordEncoder.matches(rawPassword, candidate.getPassword()))
                .orElseThrow(() -> unauthorized("Invalid email or password"));
        // Checked only once the password matched, so these states are disclosed to the owner alone.
        if (!user.isEnabled()) {
            throw unauthorized("Account is not activated");
        }
        if (user.isAccountLocked()) {
            throw unauthorized("Account is locked");
        }
        Jwt jwt = jwtService.issue(user);
        return LoginResponse.bearer(jwt.getTokenValue(), jwt.getExpiresAt());
    }

    @Override
    @Transactional
    public void logout(Jwt jwt) {
        Instant now = clock.instant();
        Instant issuedAt = jwt.getIssuedAt() != null ? jwt.getIssuedAt() : now;
        blacklistedTokenRepository.save(new BlacklistedToken(
                Long.valueOf(jwt.getSubject()), jwt.getTokenValue(), jwt.getId(), now, issuedAt, jwt.getExpiresAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile me(Long userId) {
        return userRepository.findById(userId)
                .map(UserProfile::from)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = normalize(email);
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        // Enumeration protection: the caller gets the same answer whether or not the account exists,
        // so this endpoint cannot be used to find out which emails are registered.
        if (user.isEmpty()) {
            log.debug("Password reset requested for an unknown email");
            return;
        }
        passwordResetTokenRepository.deleteByUser(user.get());
        String token = randomToken();
        passwordResetTokenRepository.save(new PasswordResetToken(
                user.get(), token, clock.instant().plus(properties.passwordResetTokenTtl())));
        // Stands in for the reset email, like the activation link.
        log.info("Password reset token for {}: {}", normalizedEmail, token);
    }

    // noRollbackFor keeps the deletion of an expired token even though the method then throws:
    // by default a RuntimeException would roll the whole transaction back, delete included.
    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void resetPassword(String token, String newRawPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(UserServiceImpl::invalidResetToken);
        if (resetToken.isExpired(clock.instant())) {
            passwordResetTokenRepository.delete(resetToken);
            throw invalidResetToken();
        }
        resetToken.getUser().setPassword(passwordEncoder.encode(newRawPassword));
        // Deleting the token makes it single-use.
        passwordResetTokenRepository.delete(resetToken);
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

    // 32 random bytes (256 bits) from a SecureRandom, URL-safe so the value fits in a link as is.
    private static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static BusinessRuleException invalidResetToken() {
        return new BusinessRuleException("Invalid or expired password reset token");
    }
}
