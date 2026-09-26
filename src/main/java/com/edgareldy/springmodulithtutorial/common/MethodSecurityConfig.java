package com.edgareldy.springmodulithtutorial.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on method-level authorization, so {@code @PreAuthorize("hasRole(...)")} on the controllers of any module is enforced.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @PreAuthorize annotations are inert until method security is enabled. Enabling it once, in the open common module,
// applies the same role-only rules to the controllers of every module.
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
