package com.edgareldy.springmodulithtutorial.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of {@code POST /api/v1/orders}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param customerId the customer placing the order
 * @param productId  the ordered product
 * @param quantity   the ordered quantity, strictly positive
 */
public record CreateOrderRequest(
        @NotNull @Positive Long customerId,
        @NotNull @Positive Long productId,
        @NotNull @Positive @Max(10_000) Integer quantity
) {
}
