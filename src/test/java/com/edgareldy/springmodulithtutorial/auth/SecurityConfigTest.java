package com.edgareldy.springmodulithtutorial.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Checks the JWT security filter chain: public routes, 401 and 403 with an ApiResponse body, and the role hierarchy.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private RoleHierarchy roleHierarchy;

    @Autowired
    private ApiResponseAccessDeniedHandler accessDeniedHandler;

    @Test
    void _01_ShouldAnswer401WithAnErrorEnvelope_WhenAnAnonymousCallerRequestsAProtectedUrl() {
        assertThat(mvc.get().uri("/api/v1/anything"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Authentication required");
                });
    }

    @Test
    void _02_ShouldServeTheOpenApiDocument_WhenCalledWithoutCredentials() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }

    @Test
    void _03_ShouldAnswer401_WhenTheBearerTokenIsNotAValidJwt() {
        assertThat(mvc.get().uri("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.success").isEqualTo(false);
    }

    @Test
    void _04_ShouldReachTheController_WhenAPublicAuthRouteIsCalledWithoutAToken() {
        assertThat(mvc.post().uri("/api/v1/auth/login").contentType("application/json").content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void _05_ShouldGrantTheUserRole_WhenTheCallerIsAnAdmin() {
        List<SimpleGrantedAuthority> admin = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        assertThat(roleHierarchy.getReachableGrantedAuthorities(admin))
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void _06_ShouldWriteA403Envelope_WhenTheFilterChainDeniesAccess() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"success\":false", "\"message\":\"Access denied\"");
    }
}
