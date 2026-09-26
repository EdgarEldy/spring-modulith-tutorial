package com.edgareldy.springmodulithtutorial.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Places and reads orders.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface OrderService {

    /**
     * Validates the customer and the product, snapshots the total, persists the order and publishes
     * an {@link OrderPlacedEvent}.
     *
     * @param request the order to place
     * @return the placed order
     */
    OrderResponse create(CreateOrderRequest request);

    /**
     * @param id the order identifier
     * @return the order
     */
    OrderResponse findById(Long id);

    /**
     * @param pageable the requested page
     * @return one page of orders
     */
    Page<OrderResponse> findAll(Pageable pageable);
}
