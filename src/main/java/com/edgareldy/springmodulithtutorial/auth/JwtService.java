package com.edgareldy.springmodulithtutorial.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Issues the JWTs returned by login and validates the bearer token of every authenticated request,
 * refusing a token whose {@code jti} was blacklisted by a logout.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Implementing JwtDecoder makes this class the decoder the resource server calls for each request
// carrying "Authorization: Bearer ...". Signature, expiry and issuer are checked by Nimbus, then the
// jti is looked up in blacklisted_tokens: a logged-out token is rejected even though it is still
// cryptographically valid. Everything comes from Spring Security's Nimbus support, no JWT library.
@Service
public class JwtService implements JwtDecoder {

    /** Claim listing the role names of the account ({@code ADMIN}, {@code USER}). */
    public static final String ROLES_CLAIM = "roles";

    /** Claim carrying the email of the account, for readability of the token only. */
    public static final String EMAIL_CLAIM = "email";

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final AuthProperties.Jwt settings;
    private final Clock clock;

    /**
     * @param properties                 the module settings, providing the key, issuer and lifetime
     * @param blacklistedTokenRepository the revoked tokens
     */
    @Autowired
    public JwtService(AuthProperties properties, BlacklistedTokenRepository blacklistedTokenRepository) {
        this(properties, blacklistedTokenRepository, Clock.systemUTC());
    }

    JwtService(AuthProperties properties, BlacklistedTokenRepository blacklistedTokenRepository, Clock clock) {
        this.settings = properties.jwt();
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.clock = clock;
        SecretKey key = hmacKey(settings.secret());
        // HS256: the same secret signs and verifies. Fine for a single application issuing tokens to
        // itself; an asymmetric key pair would be needed if other services had to verify them.
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator, new JwtIssuerValidator(settings.issuer())));
    }

    /**
     * Signs a token for the account: its id as subject, its roles, a random {@code jti} for revocation.
     *
     * @param user the authenticated account
     * @return the signed token, with its compact value and expiry
     */
    public Jwt issue(User user) {
        Instant now = clock.instant();
        List<String> roles = user.getRoles().stream().map(Role::getRoleName).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(settings.ttl()))
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLES_CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims));
    }

    /**
     * Validates a compact token: signature, expiry, issuer, then revocation.
     *
     * @param token the compact JWT
     * @return the decoded token
     * @throws JwtException if the token is invalid, expired or revoked
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = decoder.decode(token);
        if (jwt.getId() == null || blacklistedTokenRepository.existsByJti(jwt.getId())) {
            throw new BadJwtException("The token has been revoked");
        }
        return jwt;
    }

    private static SecretKey hmacKey(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.auth.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
