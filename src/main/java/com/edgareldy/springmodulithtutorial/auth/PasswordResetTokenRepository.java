package com.edgareldy.springmodulithtutorial.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link PasswordResetToken}s. A token is deleted once used, which is what makes it single-use.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * @param token the value submitted to reset-password
     * @return the token, empty if unknown or already consumed
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Discards every pending reset token of an account, so only the latest request stays valid.
     *
     * @param user the account
     */
    void deleteByUser(User user);
}
