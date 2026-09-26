package com.edgareldy.springmodulithtutorial.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the seeded roles by name.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * @param roleName {@link Role#ADMIN} or {@link Role#USER}
     * @return the role, empty if it was never seeded
     */
    Optional<Role> findByRoleName(String roleName);
}
