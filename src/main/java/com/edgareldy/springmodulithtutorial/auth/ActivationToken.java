package com.edgareldy.springmodulithtutorial.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Single-use token sent (logged) at registration; following its link enables the account.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Entity
@Table(name = "activation_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    /**
     * @param user      the account to activate
     * @param token     the random, unguessable value carried by the link
     * @param createdAt when it was issued
     * @param expiresAt after this instant the link no longer works
     */
    public ActivationToken(User user, String token, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * @param now the current instant
     * @return whether the token can still be used: never validated and not expired
     */
    public boolean isUsable(Instant now) {
        return validatedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Marks the token as consumed, so the same link cannot be replayed.
     *
     * @param now the current instant
     */
    public void markValidated(Instant now) {
        this.validatedAt = now;
    }
}
