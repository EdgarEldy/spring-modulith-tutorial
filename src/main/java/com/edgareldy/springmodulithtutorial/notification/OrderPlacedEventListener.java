package com.edgareldy.springmodulithtutorial.notification;

import com.edgareldy.springmodulithtutorial.order.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Notifies that an order was placed. The notification is simulated by a log line standing in for an email.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Public because it is the whole API surface of the notification module: nothing else lives here.
@Component
public class OrderPlacedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventListener.class);

    /**
     * @param event the order that was just placed
     */
    // @ApplicationModuleListener combines three things: the listener runs only after the publishing
    // transaction has committed (no notification for an order that was rolled back), asynchronously (placing
    // the order does not wait for it), and in a transaction of its own. The Event Publication Registry has
    // already recorded the event for this listener in event_publication; the row is marked completed only
    // when this method returns normally. If it throws, or the application stops first, the publication stays
    // incomplete and is delivered again on the next restart: at-least-once delivery, without a broker.
    @ApplicationModuleListener
    public void on(OrderPlacedEvent event) {
        log.info("Notification: order {} placed by customer {} for product {} (quantity {}, total {})",
                event.orderId(), event.customerId(), event.productId(), event.quantity(), event.total());
    }
}
