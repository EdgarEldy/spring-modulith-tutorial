package com.edgareldy.springmodulithtutorial.catalog.api;

import java.math.BigDecimal;

/**
 * Read-only view of a product handed to other modules instead of the {@code Product} entity.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id         the product identifier
 * @param name       the product name
 * @param unitPrice  the current unit price
 * @param categoryId the identifier of the product's category
 */
// An immutable copy, not a managed entity: a caller in another module can neither change catalog
// data through it nor trigger lazy loading, and the entity can evolve without breaking the caller.
public record ProductSummary(
        Long id,
        String name,
        BigDecimal unitPrice,
        Long categoryId
) {
}
