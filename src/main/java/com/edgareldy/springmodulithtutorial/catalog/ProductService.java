package com.edgareldy.springmodulithtutorial.catalog;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use cases on products: creation inside an existing category and paginated listing.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface ProductService {

    /**
     * @param name       the product name
     * @param unitPrice  the unit price
     * @param categoryId the category the product belongs to
     * @return the persisted product
     * @throws com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException if the category does not exist
     */
    Product create(String name, BigDecimal unitPrice, Long categoryId);

    /**
     * @param categoryId an optional category filter, {@code null} for every product
     * @param pageable   the requested page
     * @return one page of products
     */
    Page<Product> findAll(Long categoryId, Pageable pageable);
}
