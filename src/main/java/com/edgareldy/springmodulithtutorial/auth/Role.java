package com.edgareldy.springmodulithtutorial.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One of the two roles of the role-only authorization model ({@code ADMIN}, {@code USER}), seeded by the V1 migration.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Public in Java only because impl/UserServiceImpl, in a sub-package, needs it. For Spring Modulith
// it stays internal to auth: no other module may reference it, and verify() fails if one does.
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    /** Name of the administrator role, checked with {@code hasRole('ADMIN')}. */
    public static final String ADMIN = "ADMIN";

    /** Name of the role every registered account receives, checked with {@code hasRole('USER')}. */
    public static final String USER = "USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;
}
