package com.edgareldy.springmodulithtutorial.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.order.CreateOrderRequest;
import com.edgareldy.springmodulithtutorial.order.OrderResponse;
import com.edgareldy.springmodulithtutorial.order.OrderService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test of the Event Publication Registry: placing an order records a publication that the
 * notification listener completes.
 * <p>
 * Created edgar.muhamyangabo on 9/27/26
 * Author : edgar.muhamyangabo
 * Date : 9/27/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as the other HTTP tests on purpose: Spring reuses the cached context and its single
// PostgreSQL container. The whole application runs, so the real OrderService publishes the real event
// and the real OrderPlacedEventListener receives it. Not @Transactional: the listener only runs after
// the order transaction commits, hence unique customer emails and category names.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class OrderPlacedEventRegistryIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void _01_ShouldCompleteThePublicationAndLogTheNotification_WhenAnOrderIsPlaced(CapturedOutput output) {
        long customerId = insertCustomer();
        long productId = insertProduct();

        OrderResponse order = orderService.create(new CreateOrderRequest(customerId, productId, 2));

        // Awaitility polls a condition until it holds or the timeout expires. It replaces a fixed sleep,
        // which would be either too short on a slow machine or a waste of time on a fast one: the listener
        // runs asynchronously, after the commit of the order transaction, on another thread.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<Map<String, Object>> rows = publicationsOf(order.id());
            assertThat(rows).hasSize(1);
            assertThat((String) rows.get(0).get("listener_id")).contains("OrderPlacedEventListener.on(");
            assertThat(rows.get(0).get("completion_date")).isNotNull();
        });
        assertThat(output.getOut())
                .contains("Notification: order " + order.id() + " placed by customer " + customerId);
    }

    private List<Map<String, Object>> publicationsOf(Long orderId) {
        return jdbc.queryForList(
                "SELECT listener_id, completion_date FROM event_publication WHERE serialized_event LIKE ?",
                "%\"orderId\":" + orderId + ",%");
    }

    private long insertCustomer() {
        return jdbc.queryForObject("""
                INSERT INTO customers (first_name, last_name, telephone, email, address)
                VALUES ('Jane', 'Doe', '+250788123456', ?, '1 Main Street') RETURNING id
                """, Long.class, "notification-registry-" + UUID.randomUUID() + "@example.com");
    }

    private long insertProduct() {
        Long categoryId = jdbc.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id",
                Long.class, "notification-registry-" + UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, 'Clean Code', ?) RETURNING id",
                Long.class, categoryId, new BigDecimal("19.99"));
    }
}
