package com.edgareldy.springmodulithtutorial.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.catalog.CategoryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * HTTP tests of {@code /api/v1/categories}: envelopes, validation, business rule and role checks.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as the other integration tests so the cached context and its container are reused.
// Not @Transactional on purpose: each request runs like in production (open-in-view disabled), so the
// rows written here stay in the shared database and every name is made unique with a UUID.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryControllerTest {

    private static final String URL = "/api/v1/categories";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CategoryService categoryService;

    // @WithMockUser puts an already authenticated user with the given roles in the SecurityContext of
    // the test, bypassing any login mechanism. It isolates what these tests check (the @PreAuthorize
    // rules and the controller) from how a caller authenticates, which the auth module will change.
    @Test
    @WithMockUser(roles = "USER")
    void _01_ShouldReturnAPageOfCategories_WhenAUserListsThem() {
        String name = uniqueName("Books");
        categoryService.create(name);

        MvcTestResult result = mvc.get().uri(URL).param("page", "0").param("size", "100").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.data.page").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.data.size").isEqualTo(100);
        assertThat(result).bodyJson().extractingPath("$.data.totalElements").asNumber().isNotEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.data.totalPages").asNumber().isNotEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.data.content[*].name").asArray().contains(name);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _02_ShouldCreateTheCategoryWith201_WhenAnAdminPostsAValidName() {
        String name = uniqueName("Games");

        MvcTestResult result = postCategory("{\"name\":\"" + name + "\"}");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Category created");
        assertThat(result).bodyJson().extractingPath("$.data.id").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo(name);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _03_ShouldAnswer400WithTheInvalidField_WhenTheNameIsBlank() {
        MvcTestResult result = postCategory("{\"name\":\"  \"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Validation failed");
        assertThat(result).bodyJson().extractingPath("$.data.name").isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _04_ShouldAnswer422_WhenTheCategoryNameIsAlreadyUsed() {
        String name = uniqueName("Music");
        categoryService.create(name);

        MvcTestResult result = postCategory("{\"name\":\"" + name + "\"}");

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.message")
                .isEqualTo("A category named '" + name + "' already exists");
    }

    @Test
    @WithMockUser(roles = "USER")
    void _05_ShouldAnswer400_WhenThePageSizeIsZero() {
        assertBadRequest(mvc.get().uri(URL).param("size", "0").exchange());
    }

    @Test
    @WithMockUser(roles = "USER")
    void _06_ShouldAnswer400_WhenThePageSizeIsAbove100() {
        assertBadRequest(mvc.get().uri(URL).param("size", "101").exchange());
    }

    @Test
    @WithMockUser(roles = "USER")
    void _07_ShouldAnswer400_WhenThePageIndexIsNegative() {
        assertBadRequest(mvc.get().uri(URL).param("page", "-1").exchange());
    }

    @Test
    void _08_ShouldAnswer401_WhenAnAnonymousCallerListsCategories() {
        // Status only: the body of a 401 belongs to the security filter chain, which the auth module replaces.
        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void _09_ShouldAnswer401_WhenAnAnonymousCallerCreatesACategory() {
        assertThat(postCategory("{\"name\":\"" + uniqueName("Anonymous") + "\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "USER")
    void _10_ShouldAnswer403_WhenAUserCreatesACategory() {
        assertThat(postCategory("{\"name\":\"" + uniqueName("Forbidden") + "\"}")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _11_ShouldAnswer403_WhenAnAdminWithoutTheUserRoleListsCategories() {
        // No role hierarchy is configured: hasRole('USER') requires ROLE_USER itself, and ROLE_ADMIN
        // does not imply it.
        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult postCategory(String json) {
        return mvc.post().uri(URL).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static void assertBadRequest(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.message").asString().isNotBlank();
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
