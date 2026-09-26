package com.edgareldy.springmodulithtutorial.auth;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of revoked JWTs, queried by {@code jti} on every authenticated request.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    /**
     * Backed by the unique index on {@code jti}, so this check stays cheap on every request.
     *
     * @param jti the token id
     * @return whether that token was revoked
     */
    boolean existsByJti(String jti);
}
