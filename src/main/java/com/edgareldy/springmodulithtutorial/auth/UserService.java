package com.edgareldy.springmodulithtutorial.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Account lifecycle of the auth module: registration, activation, login, logout, profile and password reset.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Contract at the module root, implementation in impl/: the controller depends on this interface only.
// Public in Java for web/ and impl/, still internal to auth for Spring Modulith (not in api/).
public interface UserService {

    /**
     * Creates a disabled account with the USER role and logs its activation link.
     *
     * @param firstName   first name
     * @param lastName    last name
     * @param email       login, normalized to lower case
     * @param rawPassword the password as typed, hashed before storage
     * @return the created account
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException if the email is taken
     */
    UserProfile register(String firstName, String lastName, String email, String rawPassword);

    /**
     * Enables the account owning a valid activation token and consumes the token.
     *
     * @param token the value from the activation link
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException if unknown, expired or used
     */
    void activate(String token);

    /**
     * Checks the credentials and issues a JWT.
     *
     * @param email       login
     * @param rawPassword password as typed
     * @return the token to send on the next requests
     * @throws org.springframework.web.server.ResponseStatusException 401 on bad credentials, disabled or locked account
     */
    LoginResponse login(String email, String rawPassword);

    /**
     * Revokes the token of the current request by blacklisting its {@code jti}.
     *
     * @param jwt the authenticated token
     */
    void logout(Jwt jwt);

    /**
     * @param userId the authenticated account id
     * @return its profile
     * @throws com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException if the account no longer exists
     */
    UserProfile me(Long userId);

    /**
     * Issues (logs) a password reset token if the account exists; behaves identically otherwise.
     *
     * @param email login
     */
    void forgotPassword(String email);

    /**
     * Replaces the password of the account owning a valid reset token and consumes the token.
     *
     * @param token          the reset token
     * @param newRawPassword the new password as typed
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException if unknown or expired
     */
    void resetPassword(String token, String newRawPassword);
}
