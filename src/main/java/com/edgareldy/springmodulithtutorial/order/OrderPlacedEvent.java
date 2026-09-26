package com.edgareldy.springmodulithtutorial.order;

import java.math.BigDecimal;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.modulith.NamedInterface;

/**
 * Published once an order has been persisted; the notification module reacts to it.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param orderId    the identifier of the persisted order
 * @param customerId the customer who placed it
 * @param productId  the ordered product
 * @param quantity   the ordered quantity
 * @param total      the total snapshot stored on the order
 */
// An event is precisely what is meant to cross a module boundary: order announces a fact, named in the
// past tense, without knowing who listens. It carries plain values rather than the Order entity, so a
// listener can never lazily load or modify order data, and so it can be serialized into the Event
// Publication Registry and replayed later.
// @NamedInterface on the type itself publishes this single record as the "events" interface of order.
// A listening module can then declare allowedDependencies = "order :: events" and be limited to the
// event, even though the entity and repository of order are also public types of its base package.
// @DomainEvent (jMolecules) states in the code that this type is an event. Spring Modulith reads it to list
// OrderPlacedEvent under "Events published" in the generated canvas of the order module.
@DomainEvent
@NamedInterface("events")
public record OrderPlacedEvent(
        Long orderId,
        Long customerId,
        Long productId,
        int quantity,
        BigDecimal total
) {
}
