package com.edgareldy.springmodulithtutorial.auth;

import java.util.List;

/**
 * Public view of an account returned by register and /me: never the entity, never the password hash.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id        the account id, also the {@code sub} claim of its tokens
 * @param firstName first name
 * @param lastName  last name
 * @param email     login
 * @param roles     role names, sorted
 */
public record UserProfile(Long id, String firstName, String lastName, String email, List<String> roles) {

    /**
     * @param user the account
     * @return its public view
     */
    public static UserProfile from(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getRoleName).sorted().toList();
        return new UserProfile(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), roles);
    }
}
