package com.edgareldy.springmodulithtutorial.auth.api;

import java.util.Optional;

/**
 * What other modules may ask the auth module: plain ids and booleans, never the {@code User} entity.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface AuthApi {

    /**
     * @param userId an account id
     * @return whether an account with this id exists
     */
    boolean userExists(Long userId);

    /**
     * @return the id of the account authenticated on the current request, empty for an anonymous caller
     */
    Optional<Long> currentUserId();
}
