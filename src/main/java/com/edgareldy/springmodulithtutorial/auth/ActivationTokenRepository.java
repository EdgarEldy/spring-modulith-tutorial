package com.edgareldy.springmodulithtutorial.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link ActivationToken}s, looked up by the value carried in the activation link.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {

    /**
     * @param token the value from the link
     * @return the token, empty if the value is unknown
     */
    Optional<ActivationToken> findByToken(String token);
}
