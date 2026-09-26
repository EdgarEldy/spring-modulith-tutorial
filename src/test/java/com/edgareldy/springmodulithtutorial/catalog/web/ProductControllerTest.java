package com.edgareldy.springmodulithtutorial.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.catalog.CategoryService;
import com.edgareldy.springmodulithtutorial.catalog.ProductService;
import java.math.BigDecimal;
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
 * HTTP tests of {@code /api/v1/products}: category filter, validation, unknown category and role checks.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Not @Transactional: the list endpoint maps product.getCategory().getId() after the service
// transaction has ended (open-in-view is disabled), which is exactly the path production takes.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductControllerTest {

    private static final String URL = "/api/v1/products";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    @WithMockUser(roles = "USER")
    void _01_ShouldReturnOnlyTheProductsOfTheCategory_WhenAUserFiltersByCategoryId() {
        Long books = categoryService.create(uniqueName("Books")).getId();
        Long games = categoryService.create(uniqueName("Games")).getId();
        productService.create("Clean Code", new BigDecimal("39.90"), books);
        productService.create("Refactoring", new BigDecimal("45.00"), books);
        productService.create("Chess", new BigDecimal("15.00"), games);

        MvcTestResult result = mvc.get().uri(URL).param("categoryId", books.toString()).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.data.page").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.data.size").isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.data.totalPages").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.data.content[*].name").asArray()
                .containsExactly("Clean Code", "Refactoring");
        // The category of each product is a lazy proxy loaded outside any transaction by now: reading
        // its id must not need a database round trip, or this request would fail.
        assertThat(result).bodyJson().extractingPath("$.data.content[*].categoryId").asArray()
                .containsOnly(books.intValue());
    }

    @Test
    @WithMockUser(roles = "USER")
    void _02_ShouldListProductsOfEveryCategory_WhenNoCategoryIdIsGiven() {
        Long books = categoryService.create(uniqueName("Books")).getId();
        productService.create("Domain-Driven Design", new BigDecimal("55.00"), books);

        MvcTestResult result = mvc.get().uri(URL).param("size", "100").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.data.totalElements").asNumber().isNotEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _03_ShouldCreateTheProductWith201_WhenAnAdminPostsAValidProduct() {
        Long books = categoryService.create(uniqueName("Books")).getId();

        MvcTestResult result = postProduct(productJson("Clean Code", "39.90", books));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Product created");
        assertThat(result).bodyJson().extractingPath("$.data.id").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("Clean Code");
        assertThat(result).bodyJson().extractingPath("$.data.unitPrice").convertTo(BigDecimal.class)
                .satisfies(price -> assertThat(price).isEqualByComparingTo("39.90"));
        assertThat(result).bodyJson().extractingPath("$.data.categoryId").isEqualTo(books.intValue());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _04_ShouldAnswer400WithTheInvalidField_WhenTheNameIsBlank() {
        MvcTestResult result = postProduct(productJson(" ", "39.90", 1L));

        assertValidationFailedOn(result, "name");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _05_ShouldAnswer400WithTheInvalidField_WhenThePriceIsNegative() {
        assertValidationFailedOn(postProduct(productJson("Clean Code", "-1.00", 1L)), "unitPrice");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _06_ShouldAnswer400WithTheInvalidField_WhenThePriceIsZero() {
        assertValidationFailedOn(postProduct(productJson("Clean Code", "0", 1L)), "unitPrice");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _07_ShouldAnswer404_WhenTheCategoryDoesNotExist() {
        MvcTestResult result = postProduct(productJson("Clean Code", "39.90", Long.MAX_VALUE));

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.message")
                .isEqualTo("Category with id " + Long.MAX_VALUE + " not found");
    }

    @Test
    @WithMockUser(roles = "USER")
    void _08_ShouldAnswer400_WhenThePageSizeIsZero() {
        assertBadRequest(mvc.get().uri(URL).param("size", "0").exchange());
    }

    @Test
    @WithMockUser(roles = "USER")
    void _09_ShouldAnswer400_WhenThePageSizeIsAbove100() {
        assertBadRequest(mvc.get().uri(URL).param("size", "101").exchange());
    }

    @Test
    void _10_ShouldAnswer401_WhenAnAnonymousCallerListsProducts() {
        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void _11_ShouldAnswer401_WhenAnAnonymousCallerCreatesAProduct() {
        assertThat(postProduct(productJson("Clean Code", "39.90", 1L))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "USER")
    void _12_ShouldAnswer403_WhenAUserCreatesAProduct() {
        assertThat(postProduct(productJson("Clean Code", "39.90", 1L))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _13_ShouldAnswer403_WhenAnAdminWithoutTheUserRoleListsProducts() {
        // No role hierarchy: ROLE_ADMIN does not imply the ROLE_USER that hasRole('USER') requires.
        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult postProduct(String json) {
        return mvc.post().uri(URL).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String productJson(String name, String unitPrice, Long categoryId) {
        return """
                {"name":"%s","unitPrice":%s,"categoryId":%d}
                """.formatted(name, unitPrice, categoryId);
    }

    private static void assertValidationFailedOn(MvcTestResult result, String field) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Validation failed");
        assertThat(result).bodyJson().extractingPath("$.data." + field).isNotNull();
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
