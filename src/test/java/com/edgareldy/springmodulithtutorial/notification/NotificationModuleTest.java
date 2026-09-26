package com.edgareldy.springmodulithtutorial.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.order.OrderPlacedEvent;
import com.edgareldy.springmodulithtutorial.order.OrderService;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

/**
 * Module test of notification: the listener reacts to an OrderPlacedEvent without the order module running.
 * <p>
 * Created edgar.muhamyangabo on 9/27/26
 * Author : edgar.muhamyangabo
 * Date : 9/27/26
 * Project : spring-modulith-tutorial
 */
// STANDALONE (the default mode) is enough: notification only depends on the OrderPlacedEvent record, a
// plain type, not on any bean of order. Only OrderPlacedEventListener and Spring Boot's auto-configuration
// are booted; order, its service and its persistence are not. The PostgreSQL container is still needed
// because the Event Publication Registry (spring-modulith-starter-jpa) stores publications in
// event_publication, created by Flyway.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class NotificationModuleTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void _01_ShouldBootTheListenerButNoOrderBean_WhenNotificationIsUnderTest() {
        assertThat(context.getBeanNamesForType(OrderPlacedEventListener.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(OrderService.class)).isEmpty();
    }

    // Scenario.publish(...) publishes the event inside a transaction and commits it, which is what an
    // @ApplicationModuleListener waits for. andWaitForStateChange(...) then polls the supplier until the
    // acceptance predicate holds: the asynchronous listener gets time to run without any fixed sleep.
    // Publishing the event by hand, instead of placing an order, is what lets this test stand alone.
    @Test
    void _02_ShouldLogTheNotification_WhenAnOrderPlacedEventIsPublished(Scenario scenario, CapturedOutput output) {
        long orderId = ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE);
        String expected = "Notification: order " + orderId + " placed by customer 7 for product 8 (quantity 2, total 39.98)";

        scenario.publish(new OrderPlacedEvent(orderId, 7L, 8L, 2, new BigDecimal("39.98")))
                .andWaitForStateChange(() -> output.getOut().contains(expected), Boolean.TRUE::equals)
                .andVerify(logged -> assertThat(logged).isTrue());
    }
}
