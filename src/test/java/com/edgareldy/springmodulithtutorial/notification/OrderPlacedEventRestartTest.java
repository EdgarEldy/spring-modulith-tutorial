package com.edgareldy.springmodulithtutorial.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.edgareldy.springmodulithtutorial.SpringModulithTutorialApplication;
import com.edgareldy.springmodulithtutorial.order.CreateOrderRequest;
import com.edgareldy.springmodulithtutorial.order.OrderPlacedEvent;
import com.edgareldy.springmodulithtutorial.order.OrderService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves at-least-once delivery without a broker: a publication left incomplete by a failing listener is
 * delivered again when the application restarts on the same database.
 * <p>
 * Created edgar.muhamyangabo on 9/27/26
 * Author : edgar.muhamyangabo
 * Date : 9/27/26
 * Project : spring-modulith-tutorial
 */
// A restart cannot be simulated inside one Spring test context, so this class owns its database and starts
// the application twice by hand, one context after the other, on that same database. The container is
// managed by the Testcontainers JUnit extension (@Testcontainers + static @Container: started once before
// the tests of this class, stopped after them) instead of TestcontainersConfiguration, whose container
// lives and dies with a single Spring context.
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class OrderPlacedEventRestartTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Test
    void _01_ShouldRedeliverTheIncompletePublication_WhenTheApplicationRestartsAfterAListenerFailure(
            CapturedOutput output) {
        long orderId;
        String publicationId;
        String listenerId;

        // Run #1: the listener throws. OrderService commits the order and, in the same transaction, the
        // registry inserts one event_publication row per @ApplicationModuleListener. After the commit, the
        // listener runs asynchronously and fails, so the row is never marked completed: completion_date
        // stays NULL and the event is not lost, it waits in the database.
        try (ConfigurableApplicationContext first = start(true)) {
            JdbcTemplate jdbc = first.getBean(JdbcTemplate.class);
            orderId = first.getBean(OrderService.class)
                    .create(new CreateOrderRequest(insertCustomer(jdbc), insertProduct(jdbc), 2))
                    .id();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                List<Map<String, Object>> rows = publicationsOf(jdbc, orderId);
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).get("status")).isEqualTo("FAILED");
                assertThat(rows.get(0).get("completion_date")).isNull();
            });
            Map<String, Object> failed = publicationsOf(jdbc, orderId).get(0);
            publicationId = failed.get("id").toString();
            listenerId = (String) failed.get("listener_id");
        }
        // The spy is an instance of the real class (Mockito's inline mock maker does not subclass), so the
        // listener id recorded during run #1 is the one of the real listener, which run #2 can match.
        assertThat(listenerId)
                .isEqualTo(OrderPlacedEventListener.class.getName() + ".on(" + OrderPlacedEvent.class.getName() + ")");
        assertThat(output.getOut()).doesNotContain("Notification: order " + orderId + " placed");

        // Run #2: the real listener, same database. spring.modulith.events.republish-outstanding-events-on-restart
        // (application.yml) makes the registry look for incomplete publications at startup and submit them
        // again to their listener. The same row (same id) is then marked completed: the notification is
        // delivered once the failure is gone, without a message broker.
        try (ConfigurableApplicationContext second = start(false)) {
            JdbcTemplate jdbc = second.getBean(JdbcTemplate.class);

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                List<Map<String, Object>> rows = publicationsOf(jdbc, orderId);
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).get("id").toString()).isEqualTo(publicationId);
                assertThat(rows.get(0).get("completion_date")).isNotNull();
            });
            assertThat(output.getOut()).contains("Notification: order " + orderId + " placed");
        }
    }

    // SpringApplicationBuilder starts the real application the way main() does, outside the Spring test
    // framework, so each call is an independent context that can be closed to simulate a stop. The
    // datasource is passed as command-line arguments pointing at this class's container, and server.port=0
    // avoids a clash on 8080 with anything already running.
    private static ConfigurableApplicationContext start(boolean failingListener) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringModulithTutorialApplication.class)
                .initializers(context -> context.getBeanFactory()
                        .registerSingleton("excludeTestConfigurations", new ExcludeTestConfigurations()));
        if (failingListener) {
            builder.initializers(context -> context.getBeanFactory().addBeanPostProcessor(new FailingListener()));
        }
        return builder.run(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--server.port=0");
    }

    private static List<Map<String, Object>> publicationsOf(JdbcTemplate jdbc, long orderId) {
        return jdbc.queryForList("""
                SELECT id, listener_id, status, completion_date FROM event_publication
                WHERE serialized_event LIKE ?
                """, "%\"orderId\":" + orderId + ",%");
    }

    private static long insertCustomer(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                INSERT INTO customers (first_name, last_name, telephone, email, address)
                VALUES ('Jane', 'Doe', '+250788123456', ?, '1 Main Street') RETURNING id
                """, Long.class, "notification-restart-" + UUID.randomUUID() + "@example.com");
    }

    private static long insertProduct(JdbcTemplate jdbc) {
        Long categoryId = jdbc.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id",
                Long.class, "notification-restart-" + UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, 'Clean Code', ?) RETURNING id",
                Long.class, categoryId, new BigDecimal("19.99"));
    }

    /**
     * Replaces the real listener bean by a Mockito spy of it that throws, for run #1 only.
     * <p>
     * Created edgar.muhamyangabo on 9/27/26
     * Author : edgar.muhamyangabo
     * Date : 9/27/26
     * Project : spring-modulith-tutorial
     */
    // Registered directly on the bean factory rather than as a scanned bean, so run #2 never sees it. It
    // keeps the bean name and the class of the real listener: only the behavior of on(...) changes.
    private static final class FailingListener implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof OrderPlacedEventListener listener) {
                OrderPlacedEventListener spy = Mockito.spy(listener);
                doThrow(new IllegalStateException("Simulated notification failure")).when(spy).on(any());
                return spy;
            }
            return bean;
        }
    }

    /**
     * Keeps the test-only configurations out of the component scan of a context started by hand.
     * <p>
     * Created edgar.muhamyangabo on 9/27/26
     * Author : edgar.muhamyangabo
     * Date : 9/27/26
     * Project : spring-modulith-tutorial
     */
    // @SpringBootApplication scans the whole base package, test classes included. Inside the Spring test
    // framework a filter skips @TestConfiguration classes; outside of it, TestcontainersConfiguration would
    // be picked up and its @ServiceConnection container would replace the datasource given above.
    // @SpringBootApplication delegates to every TypeExcludeFilter bean, hence this one.
    private static final class ExcludeTestConfigurations extends TypeExcludeFilter {

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            return metadataReader.getAnnotationMetadata().isAnnotated(TestConfiguration.class.getName());
        }
    }
}
