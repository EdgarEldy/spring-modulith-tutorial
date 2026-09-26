package com.edgareldy.springmodulithtutorial.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.catalog.api.CatalogApi;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerApi;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.util.ClassUtils;

/**
 * Module test of order: boots order with the modules it depends on and places an order end to end.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// DIRECT_DEPENDENCIES bootstraps order plus the modules it directly depends on: catalog and customer (for
// their api named interfaces) and the open common module. The real CatalogApi and CustomerApi beans are
// therefore wired into OrderServiceImpl, while notification (which depends on order, not the other way
// round) and auth (unrelated) are left out. The default STANDALONE mode would fail here, since
// OrderServiceImpl needs beans that only catalog and customer provide. Nothing security-specific is
// needed: OrderService carries no @PreAuthorize, only the controller does.
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
class OrderModuleTest {

    private static final String ROOT = "com.edgareldy.springmodulithtutorial.";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void _01_ShouldBootCatalogAndCustomerButNotNotificationOrAuth_WhenOrderIsUnderTest() {
        assertThat(context.getBeanNamesForType(CatalogApi.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(CustomerApi.class)).hasSize(1);

        List<String> packages = Arrays.stream(context.getBeanDefinitionNames())
                .map(context::getType)
                .filter(Objects::nonNull)
                .map(type -> ClassUtils.getUserClass(type).getPackageName())
                .toList();
        assertThat(packages)
                .noneMatch(name -> name.startsWith(ROOT + "notification"))
                .noneMatch(name -> name.startsWith(ROOT + "auth"));
    }

    // Scenario, injected by @ApplicationModuleTest, describes a test as stimulus then expected outcome:
    // it runs the stimulus in a transaction, waits for an event of the given type matching the given
    // criteria, then hands both the event and the stimulus result over for verification. Waiting
    // matters once listeners become asynchronous; here it also proves the event really left the module.
    @Test
    void _02_ShouldPlaceTheOrderAndPublishOrderPlacedEvent_WhenCustomerAndProductExist(Scenario scenario) {
        long customerId = insertCustomer();
        long productId = insertProduct("19.99");

        scenario.stimulate(() -> orderService.create(new CreateOrderRequest(customerId, productId, 3)))
                .andWaitForEventOfType(OrderPlacedEvent.class)
                .matching(event -> event.customerId() == customerId)
                .toArriveAndVerify((event, order) -> {
                    assertThat(event.orderId()).isEqualTo(order.id());
                    assertThat(event.productId()).isEqualTo(productId);
                    assertThat(event.quantity()).isEqualTo(3);
                    assertThat(event.total()).isEqualByComparingTo("59.97");
                    assertThat(orderService.findById(order.id()).total()).isEqualByComparingTo("59.97");
                });
    }

    // AssertablePublishedEvents records every event published during the current test method only, so
    // the absence of an event can be asserted as well as its presence.
    @Test
    void _03_ShouldPublishNoEvent_WhenTheCustomerIsUnknown(AssertablePublishedEvents events) {
        long productId = insertProduct("5.00");

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(Long.MAX_VALUE, productId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(events.ofType(OrderPlacedEvent.class)).isEmpty();
    }

    private long insertCustomer() {
        return jdbc.queryForObject("""
                INSERT INTO customers (first_name, last_name, telephone, email, address)
                VALUES ('Jane', 'Doe', '+250788123456', ?, '1 Main Street') RETURNING id
                """, Long.class, "order-module-" + UUID.randomUUID() + "@example.com");
    }

    private long insertProduct(String unitPrice) {
        Long categoryId = jdbc.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id",
                Long.class, "order-module-" + UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, 'Clean Code', ?) RETURNING id",
                Long.class, categoryId, new BigDecimal(unitPrice));
    }
}
