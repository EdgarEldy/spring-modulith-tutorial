package com.edgareldy.springmodulithtutorial.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link User} accounts, looked up by their (lower case) email.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * @param email the normalized email
     * @return the account, empty if none is registered with it
     */
    Optional<User> findByEmail(String email);

    /**
     * @param email the normalized email
     * @return whether an account already uses it
     */
    boolean existsByEmail(String email);
}
