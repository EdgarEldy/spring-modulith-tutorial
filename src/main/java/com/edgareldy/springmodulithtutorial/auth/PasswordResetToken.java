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
 * Short-lived, single-use token sent (logged) by forgot-password and consumed by reset-password.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    /** Value of the {@code type} column for a forgotten-password token, the only kind issued today. */
    public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * @param user       the account whose password may be reset
     * @param token      the random, unguessable value
     * @param expiryDate after this instant the token is refused
     */
    public PasswordResetToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.type = TYPE_PASSWORD_RESET;
        this.expiryDate = expiryDate;
    }

    /**
     * @param now the current instant
     * @return whether the token has expired
     */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiryDate);
    }
}
