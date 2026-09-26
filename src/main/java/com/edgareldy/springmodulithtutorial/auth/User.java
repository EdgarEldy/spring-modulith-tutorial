package com.edgareldy.springmodulithtutorial.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An account of the application: identity, hashed password, activation and lock flags, and its roles.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Never leaves the auth module: other modules only learn about users through auth.api.AuthApi,
// which answers with plain ids and booleans, never with this entity.
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked;

    // Roles are loaded with the user: every login and every /me needs them, and there are at most two.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_user",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /**
     * Creates an account that is neither enabled nor locked and has no role yet.
     *
     * @param firstName       first name
     * @param lastName        last name
     * @param email           login, already normalized to lower case
     * @param encodedPassword the password hashed by the {@code PasswordEncoder}, never the raw one
     */
    public User(String firstName, String lastName, String email, String encodedPassword) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = encodedPassword;
    }

    /**
     * @param role a role to grant
     */
    public void addRole(Role role) {
        roles.add(role);
    }
}
