package com.edgareldy.springmodulithtutorial.catalog.web;

import com.edgareldy.springmodulithtutorial.catalog.Product;
import java.math.BigDecimal;

/**
 * Product as returned by the HTTP API.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id         the product identifier
 * @param name       the product name
 * @param unitPrice  the unit price
 * @param categoryId the identifier of the product's category
 */
public record ProductResponse(
        Long id,
        String name,
        BigDecimal unitPrice,
        Long categoryId
) {

    /**
     * @param product the entity
     * @return its HTTP representation
     */
    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getUnitPrice(),
                product.getCategory().getId());
    }
}
