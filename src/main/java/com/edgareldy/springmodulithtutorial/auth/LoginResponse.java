package com.edgareldy.springmodulithtutorial.auth;

import java.time.Instant;

/**
 * Result of a successful login: the JWT to send as {@code Authorization: Bearer <accessToken>}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param accessToken the compact signed JWT
 * @param tokenType   always {@code Bearer}
 * @param expiresAt   when the token stops being accepted
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {

    /** The only token type issued by the application. */
    public static final String BEARER = "Bearer";

    /**
     * @param accessToken the compact signed JWT
     * @param expiresAt   its expiry
     * @return a bearer login response
     */
    public static LoginResponse bearer(String accessToken, Instant expiresAt) {
        return new LoginResponse(accessToken, BEARER, expiresAt);
    }
}
