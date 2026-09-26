package com.edgareldy.springmodulithtutorial.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the auth module, bound from the {@code app.auth} section of {@code application.yml}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param baseUrl                 public URL of the application, used to build the logged activation link
 * @param activationTokenTtl      how long an activation link stays valid
 * @param passwordResetTokenTtl   how long a password reset token stays valid
 * @param jwt                     signing and lifetime of the issued JWTs
 * @param admin                   the optional administrator account created at startup
 */
// A record bound with @ConfigurationProperties gathers every setting of the module in one typed,
// immutable object, instead of @Value strings scattered across classes.
@ConfigurationProperties("app.auth")
public record AuthProperties(
        String baseUrl,
        Duration activationTokenTtl,
        Duration passwordResetTokenTtl,
        Jwt jwt,
        Admin admin
) {

    /**
     * JWT settings.
     * <p>
     * Created edgar.muhamyangabo on 9/26/26
     * Author : edgar.muhamyangabo
     * Date : 9/26/26
     * Project : spring-modulith-tutorial
     *
     * @param secret HMAC key, at least 32 bytes for HS256
     * @param issuer value of the {@code iss} claim, checked on every incoming token
     * @param ttl    lifetime of a token
     */
    public record Jwt(String secret, String issuer, Duration ttl) {
    }

    /**
     * Administrator bootstrap settings; both values must be set for the account to be created.
     * <p>
     * Created edgar.muhamyangabo on 9/26/26
     * Author : edgar.muhamyangabo
     * Date : 9/26/26
     * Project : spring-modulith-tutorial
     *
     * @param email    login of the administrator
     * @param password its initial password
     */
    public record Admin(String email, String password) {

        /**
         * @return whether both values were provided
         */
        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }

        // Keeps the password out of logs and error messages if the record is ever printed.
        @Override
        public String toString() {
            return "Admin[email=" + email + ", password=****]";
        }
    }
}
