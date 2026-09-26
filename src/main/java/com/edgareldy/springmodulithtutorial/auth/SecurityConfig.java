package com.edgareldy.springmodulithtutorial.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security of the application: the auth endpoints needed before a login, health and API
 * documentation are public; every other request must carry a valid JWT issued by {@link JwtService}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class SecurityConfig {

    private static final String AUTH_PATH = "/api/v1/auth";

    /**
     * @param http                 the security builder
     * @param jwtService           decodes and validates the bearer tokens
     * @param entryPoint           writes the 401 envelope
     * @param accessDeniedHandler  writes the 403 envelope
     * @return the filter chain applied to every request
     * @throws Exception if the chain cannot be built
     */
    // Declaring a SecurityFilterChain bean replaces Spring Boot's default one (HTTP Basic and a
    // generated password on every URL). A REST API authenticated by tokens needs neither sessions nor
    // CSRF protection: the browser never sends the token on its own, the client adds it explicitly.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                            ApiResponseAuthenticationEntryPoint entryPoint,
                                            ApiResponseAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                AUTH_PATH + "/register", AUTH_PATH + "/login",
                                AUTH_PATH + "/forgot-password", AUTH_PATH + "/reset-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, AUTH_PATH + "/activate-account")
                        .permitAll()
                        .anyRequest().authenticated())
                // The resource server support installs a BearerTokenAuthenticationFilter: it reads
                // "Authorization: Bearer ...", hands the token to our JwtService (signature, expiry,
                // issuer, blacklist) and turns the claims into an Authentication.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtService)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Also set on exceptionHandling: a request without any token never reaches the resource
                // server filter, and must still receive the 401 envelope rather than a bare status.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    // By default the resource server maps the "scope" claim to SCOPE_xxx authorities. Our tokens carry
    // the role names in "roles" instead; prefixing them with ROLE_ is what hasRole('ADMIN') and
    // hasRole('USER') expect, so @PreAuthorize works on the raw role names stored in the database.
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtService.ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * @return the encoder hashing passwords at registration and checking them at login
     */
    // The delegating encoder stores the algorithm id in the hash ("{bcrypt}$2a$..."). BCrypt is used
    // today, and a future algorithm can be introduced without invalidating the existing hashes.
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
