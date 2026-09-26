package com.edgareldy.springmodulithtutorial.catalog.api;

import java.util.Optional;

/**
 * What other modules may ask the catalog module: whether a product exists and its current summary.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CatalogApi {

    /**
     * @param id a product identifier
     * @return the product summary, or empty if no product has this identifier
     */
    Optional<ProductSummary> findProduct(Long id);

    /**
     * @param id a product identifier
     * @return whether a product has this identifier
     */
    boolean productExists(Long id);
}
