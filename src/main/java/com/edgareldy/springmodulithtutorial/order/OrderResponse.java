package com.edgareldy.springmodulithtutorial.order;

import java.math.BigDecimal;

/**
 * JSON representation of an order.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id         the order identifier
 * @param customerId the customer who placed it
 * @param productId  the ordered product
 * @param quantity   the ordered quantity
 * @param total      the total computed when the order was placed
 */
public record OrderResponse(
        Long id,
        Long customerId,
        Long productId,
        int quantity,
        BigDecimal total
) {

    /**
     * @param order the entity
     * @return its JSON representation
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotal()
        );
    }
}
