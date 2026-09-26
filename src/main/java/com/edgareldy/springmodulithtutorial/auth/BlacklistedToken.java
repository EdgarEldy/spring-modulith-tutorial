package com.edgareldy.springmodulithtutorial.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A JWT revoked by a logout, identified by its {@code jti} claim until it would have expired anyway.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// A JWT is valid by construction until its expiry: the server keeps no session to delete. Logging
// out therefore means remembering the token's unique id (jti) and refusing it on every request.
@Entity
@Table(name = "blacklisted_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain id is enough here: the logout only knows the token's subject and never needs the user row.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 1024)
    private String token;

    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    /**
     * @param userId        the owner of the token (its {@code sub} claim)
     * @param token         the compact JWT
     * @param jti           its unique id
     * @param blacklistedAt when it was revoked
     * @param createdAt     when it was issued ({@code iat})
     * @param expiresAt     when it expires ({@code exp}); the row is useless afterwards
     */
    public BlacklistedToken(Long userId, String token, String jti, Instant blacklistedAt, Instant createdAt,
                            Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.jti = jti;
        this.blacklistedAt = blacklistedAt;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
