package com.edgareldy.springmodulithtutorial.auth.impl;

import com.edgareldy.springmodulithtutorial.auth.UserRepository;
import com.edgareldy.springmodulithtutorial.auth.api.AuthApi;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the auth module's published {@link AuthApi}, answering other modules with ids and booleans.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Other modules inject the AuthApi interface from auth.api; this class stays in impl/ and is never
// referenced outside auth. The implementation can change freely without touching any caller.
@Service
class AuthApiImpl implements AuthApi {

    private final UserRepository userRepository;

    /**
     * @param userRepository accounts
     */
    AuthApiImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userId != null && userRepository.existsById(userId);
    }

    @Override
    public Optional<Long> currentUserId() {
        // The resource server stores a JwtAuthenticationToken in the SecurityContext of each request;
        // its subject is the account id written by JwtService.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(Long.valueOf(jwtAuthentication.getToken().getSubject()));
        }
        return Optional.empty();
    }
}
