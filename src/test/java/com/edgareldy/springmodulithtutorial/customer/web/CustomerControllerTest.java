package com.edgareldy.springmodulithtutorial.customer.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.customer.CreateCustomerRequest;
import com.edgareldy.springmodulithtutorial.customer.CustomerResponse;
import com.edgareldy.springmodulithtutorial.customer.CustomerService;
import com.jayway.jsonpath.JsonPath;
import java.util.Locale;
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
 * HTTP tests of {@link CustomerController}: status codes, {@code ApiResponse} bodies and role checks of both endpoints.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Not @Transactional: MockMvc requests commit their own transactions. Every customer therefore gets
// a random email, so reruns and other test classes sharing the container never collide.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerControllerTest {

    private static final String CUSTOMERS = "/api/v1/customers";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CustomerService customerService;

    // @WithMockUser puts an already authenticated user with the given roles into the security context
    // for the duration of the test, so @PreAuthorize can be exercised without issuing a real token.
    // The status of the response then only depends on the role rule, not on a login mechanism.
    @Test
    @WithMockUser(roles = "ADMIN")
    void _01_ShouldCreateTheCustomerWithLocation_WhenAnAdminPostsAValidBody() throws Exception {
        String email = "New-" + UUID.randomUUID() + "@Example.com";

        MvcTestResult result = mvc.post().uri(CUSTOMERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Jane", "Doe", "+250788123456", email, "1 Main Street"))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.id").isNotNull();
                    json.assertThat().extractingPath("$.data.email").isEqualTo(email.toLowerCase(Locale.ROOT));
                    json.assertThat().extractingPath("$.data.firstName").isEqualTo("Jane");
                });
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        assertThat(result).headers().hasValue("Location", CUSTOMERS + "/" + id);
    }

    @Test
    @WithMockUser(roles = "USER")
    void _02_ShouldReturnTheCustomer_WhenAUserRequestsAnExistingId() {
        CustomerResponse existing = createCustomer();

        assertThat(mvc.get().uri(CUSTOMERS + "/{id}", existing.id()))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.id").asNumber().satisfies(
                            id -> assertThat(id.longValue()).isEqualTo(existing.id()));
                    json.assertThat().extractingPath("$.data.email").isEqualTo(existing.email());
                    json.assertThat().extractingPath("$.data.telephone").isEqualTo(existing.telephone());
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void _03_ShouldReturn404Error_WhenTheIdIsUnknown() {
        assertThat(mvc.get().uri(CUSTOMERS + "/{id}", Long.MAX_VALUE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message")
                            .isEqualTo("Customer with id " + Long.MAX_VALUE + " not found");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _04_ShouldReturn400WithTheInvalidFields_WhenTheBodyIsInvalid() {
        assertThat(mvc.post().uri(CUSTOMERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(" ", "", "abc", "not-an-email", "1 Main Street")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Validation failed");
                    json.assertThat().extractingPath("$.data").asMap()
                            .containsOnlyKeys("firstName", "lastName", "telephone", "email");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _05_ShouldReturn422Error_WhenTheEmailIsAlreadyUsedWithAnotherCase() {
        CustomerResponse existing = createCustomer();

        assertThat(mvc.post().uri(CUSTOMERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("John", "Doe", "+250788654321", existing.email().toUpperCase(Locale.ROOT), "2 Main Street")))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message")
                            .isEqualTo("A customer with email " + existing.email() + " already exists");
                });
    }

    // The bodies of 401 and 403 are not asserted: the auth module replaces this filter chain with a
    // JWT one, and only the status codes are part of the contract at this stage.
    @Test
    void _06_ShouldReturn401_WhenAnAnonymousCallerReadsACustomer() {
        assertThat(mvc.get().uri(CUSTOMERS + "/{id}", 1L)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void _07_ShouldReturn401_WhenAnAnonymousCallerCreatesACustomer() {
        assertThat(mvc.post().uri(CUSTOMERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Jane", "Doe", "+250788123456", uniqueEmail(), "1 Main Street")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "USER")
    void _08_ShouldReturn403_WhenAUserCreatesACustomer() {
        assertThat(mvc.post().uri(CUSTOMERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Jane", "Doe", "+250788123456", uniqueEmail(), "1 Main Street")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _09_ShouldReturn403_WhenAnAdminWithoutTheUserRoleReadsACustomer() {
        // No role hierarchy is configured: ADMIN does not imply USER, so hasRole('USER') refuses it.
        CustomerResponse existing = createCustomer();

        assertThat(mvc.get().uri(CUSTOMERS + "/{id}", existing.id())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private CustomerResponse createCustomer() {
        return customerService.create(
                new CreateCustomerRequest("Jane", "Doe", "+250788123456", uniqueEmail(), "1 Main Street"));
    }

    private static String uniqueEmail() {
        return "web-" + UUID.randomUUID() + "@example.com";
    }

    private static String body(String firstName, String lastName, String telephone, String email, String address) {
        return """
                {"firstName":"%s","lastName":"%s","telephone":"%s","email":"%s","address":"%s"}
                """.formatted(firstName, lastName, telephone, email, address);
    }
}
