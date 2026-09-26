package com.edgareldy.springmodulithtutorial.order.impl;

import com.edgareldy.springmodulithtutorial.catalog.api.CatalogApi;
import com.edgareldy.springmodulithtutorial.catalog.api.ProductSummary;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerApi;
import com.edgareldy.springmodulithtutorial.order.CreateOrderRequest;
import com.edgareldy.springmodulithtutorial.order.Order;
import com.edgareldy.springmodulithtutorial.order.OrderPlacedEvent;
import com.edgareldy.springmodulithtutorial.order.OrderRepository;
import com.edgareldy.springmodulithtutorial.order.OrderResponse;
import com.edgareldy.springmodulithtutorial.order.OrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places orders through the catalog and customer public APIs and announces them with an event.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Service
@Transactional(readOnly = true)
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CatalogApi catalogApi;
    private final CustomerApi customerApi;
    private final ApplicationEventPublisher events;

    // The other modules are injected as their named-interface types only. Spring still wires the
    // implementations living inside catalog and customer, but order cannot see them, their entities or
    // their repositories: the dependency graph stays order -> catalog :: api, order -> customer :: api.
    OrderServiceImpl(OrderRepository orderRepository, CatalogApi catalogApi, CustomerApi customerApi,
                     ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.catalogApi = catalogApi;
        this.customerApi = customerApi;
        this.events = events;
    }

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        if (!customerApi.customerExists(request.customerId())) {
            throw ResourceNotFoundException.of("Customer", request.customerId());
        }
        ProductSummary product = catalogApi.findProduct(request.productId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));

        BigDecimal total = product.unitPrice()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        Order order = orderRepository.save(
                new Order(request.customerId(), request.productId(), request.quantity(), total));

        // Published inside the transaction that persisted the order. With spring-modulith-starter-jpa, the
        // Event Publication Registry stores one row per listener in event_publication as part of this same
        // transaction, so the event exists if and only if the order does, even if the application stops
        // before a listener has run.
        events.publishEvent(new OrderPlacedEvent(
                order.getId(), order.getCustomerId(), order.getProductId(), order.getQuantity(), order.getTotal()));
        return OrderResponse.from(order);
    }

    @Override
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", id));
    }

    @Override
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }
}
