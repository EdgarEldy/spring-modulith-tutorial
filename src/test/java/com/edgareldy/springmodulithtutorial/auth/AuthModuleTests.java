package com.edgareldy.springmodulithtutorial.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.auth.api.AuthApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Boots the auth module on its own and checks that it works without any other business module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @ApplicationModuleTest starts a Spring context limited to the package of this test class, the auth
// module: only its components are scanned and only its entities are known to JPA, while the Spring Boot
// auto-configurations (datasource, Flyway, security) still apply. auth depends on no other business
// module, so the default STANDALONE bootstrap mode is enough, and a success proves that no hidden
// dependency on catalog, customer or order crept in.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class AuthModuleTests {

    @Autowired
    private AuthApi authApi;

    @Autowired
    private UserService userService;

    @Test
    void _01_ShouldExposeTheRegisteredAccount_WhenAnotherModuleAsksThroughAuthApi() {
        UserProfile profile = userService.register("Ada", "Lovelace", UUID.randomUUID() + "@example.com", "password123");

        assertThat(authApi.userExists(profile.id())).isTrue();
        assertThat(authApi.userExists(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void _02_ShouldReportNoCurrentUser_WhenNoRequestIsAuthenticated() {
        assertThat(authApi.currentUserId()).isEmpty();
    }
}
