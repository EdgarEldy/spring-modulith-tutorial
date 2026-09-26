package com.edgareldy.springmodulithtutorial.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * HTTP tests of {@link OrderController}: total snapshot, 404 on unknown references, validation and role checks.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Customers, categories and products are inserted with plain SQL: this test belongs to the order module,
// so it stays away from the internal services and entities of catalog and customer, just like the
// production code. Not @Transactional (MockMvc requests commit), hence unique names and emails.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderControllerTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @WithMockUser(roles = "USER")
    void _01_ShouldPlaceTheOrderWithLocationAndComputedTotal_WhenAUserPostsAValidBody() throws Exception {
        long customerId = insertCustomer();
        long productId = insertProduct("19.99");

        MvcTestResult result = postOrder(body(customerId, productId, 3));

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.id").isNotNull();
                    json.assertThat().extractingPath("$.data.customerId").asNumber()
                            .satisfies(id -> assertThat(id.longValue()).isEqualTo(customerId));
                    json.assertThat().extractingPath("$.data.productId").asNumber()
                            .satisfies(id -> assertThat(id.longValue()).isEqualTo(productId));
                    json.assertThat().extractingPath("$.data.quantity").isEqualTo(3);
                });
        assertThat(totalOf(result)).isEqualByComparingTo("59.97");
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        assertThat(result).headers().hasValue("Location", ORDERS + "/" + id);
    }

    @Test
    @WithMockUser(roles = "USER")
    void _02_ShouldKeepTheOriginalTotal_WhenTheProductPriceChangesAfterTheOrder() throws Exception {
        long productId = insertProduct("19.99");
        MvcTestResult placed = postOrder(body(insertCustomer(), productId, 3));
        assertThat(placed).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(placed.getResponse().getContentAsString(), "$.data.id");

        int updated = jdbc.update("UPDATE products SET unit_price = ? WHERE id = ?", new BigDecimal("25.00"), productId);
        assertThat(updated).isEqualTo(1);

        MvcTestResult read = mvc.get().uri(ORDERS + "/{id}", id).exchange();
        assertThat(read).hasStatusOk();
        // The total is a snapshot stored on the orders row, not quantity x the current price (75.00).
        assertThat(totalOf(read)).isEqualByComparingTo("59.97");
    }

    @Test
    @WithMockUser(roles = "USER")
    void _03_ShouldReturn404Error_WhenTheCustomerIsUnknown() {
        assertThat(postOrder(body(Long.MAX_VALUE, insertProduct("10.00"), 1)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message")
                            .isEqualTo("Customer with id " + Long.MAX_VALUE + " not found");
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void _04_ShouldReturn404Error_WhenTheProductIsUnknown() {
        assertThat(postOrder(body(insertCustomer(), Long.MAX_VALUE, 1)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message")
                            .isEqualTo("Product with id " + Long.MAX_VALUE + " not found");
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void _05_ShouldReturn400WithTheInvalidFields_WhenTheBodyIsInvalid() {
        assertThat(postOrder("""
                {"customerId":null,"productId":-1,"quantity":0}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Validation failed");
                    json.assertThat().extractingPath("$.data").asMap()
                            .containsOnlyKeys("customerId", "productId", "quantity");
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void _06_ShouldReturn400Error_WhenTheQuantityExceedsTheMaximum() {
        assertThat(postOrder(body(1L, 1L, 10_001)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.data").asMap().containsOnlyKeys("quantity");
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void _07_ShouldReturnTheOrder_WhenAUserRequestsAnExistingId() throws Exception {
        long customerId = insertCustomer();
        MvcTestResult placed = postOrder(body(customerId, insertProduct("4.50"), 2));
        Number id = JsonPath.read(placed.getResponse().getContentAsString(), "$.data.id");

        MvcTestResult read = mvc.get().uri(ORDERS + "/{id}", id).exchange();

        assertThat(read).hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.id").isEqualTo(id);
                    json.assertThat().extractingPath("$.data.customerId").asNumber()
                            .satisfies(value -> assertThat(value.longValue()).isEqualTo(customerId));
                    json.assertThat().extractingPath("$.data.quantity").isEqualTo(2);
                });
        assertThat(totalOf(read)).isEqualByComparingTo("9.00");
    }

    @Test
    @WithMockUser(roles = "USER")
    void _08_ShouldReturn404Error_WhenTheOrderIdIsUnknown() {
        assertThat(mvc.get().uri(ORDERS + "/{id}", Long.MAX_VALUE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message")
                            .isEqualTo("Order with id " + Long.MAX_VALUE + " not found");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _09_ShouldReturnAPageOfOrders_WhenAnAdminListsThem() {
        insertOrder();

        // Other test classes also write orders into the shared container, so only the shape and a lower
        // bound are asserted, not an exact count.
        assertThat(mvc.get().uri(ORDERS).param("page", "0").param("size", "1"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.page").isEqualTo(0);
                    json.assertThat().extractingPath("$.data.size").isEqualTo(1);
                    json.assertThat().extractingPath("$.data.content").asArray().hasSize(1);
                    json.assertThat().extractingPath("$.data.totalElements").asNumber()
                            .satisfies(total -> assertThat(total.longValue()).isPositive());
                    json.assertThat().extractingPath("$.data.content[0].total").isNotNull();
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _10_ShouldReturn400Error_WhenThePageSizeIsOutOfRange() {
        assertThat(mvc.get().uri(ORDERS).param("size", "101"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.success").isEqualTo(false);
    }

    // As in the other modules, 401 and 403 are asserted by status only.
    @Test
    @WithMockUser(roles = "USER")
    void _11_ShouldReturn403_WhenAUserListsOrders() {
        assertThat(mvc.get().uri(ORDERS)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void _12_ShouldReturn401_WhenAnAnonymousCallerPlacesAnOrder() {
        assertThat(postOrder(body(1L, 1L, 1))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _13_ShouldReturn403_WhenAnAdminWithoutTheUserRolePlacesAnOrder() {
        // No role hierarchy: ADMIN does not imply USER, so hasRole('USER') refuses it.
        assertThat(postOrder(body(insertCustomer(), insertProduct("10.00"), 1))).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult postOrder(String json) {
        return mvc.post().uri(ORDERS).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static BigDecimal totalOf(MvcTestResult result) throws Exception {
        Object total = JsonPath.read(result.getResponse().getContentAsString(), "$.data.total");
        return new BigDecimal(total.toString());
    }

    private long insertCustomer() {
        return jdbc.queryForObject("""
                INSERT INTO customers (first_name, last_name, telephone, email, address)
                VALUES ('Jane', 'Doe', '+250788123456', ?, '1 Main Street') RETURNING id
                """, Long.class, "order-web-" + UUID.randomUUID() + "@example.com");
    }

    private long insertProduct(String unitPrice) {
        Long categoryId = jdbc.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id",
                Long.class, "order-web-" + UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, 'Clean Code', ?) RETURNING id",
                Long.class, categoryId, new BigDecimal(unitPrice));
    }

    private void insertOrder() {
        jdbc.update("INSERT INTO orders (customer_id, product_id, quantity, total) VALUES (?, ?, 1, 10.00)",
                insertCustomer(), insertProduct("10.00"));
    }

    private static String body(long customerId, long productId, int quantity) {
        return """
                {"customerId":%d,"productId":%d,"quantity":%d}
                """.formatted(customerId, productId, quantity);
    }
}
