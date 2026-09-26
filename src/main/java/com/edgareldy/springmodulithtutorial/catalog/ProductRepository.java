package com.edgareldy.springmodulithtutorial.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link Product}, internal to the catalog module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * @param categoryId the category to filter on
     * @param pageable   the requested page
     * @return one page of the products of that category
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * @param categoryId a category identifier
     * @return whether at least one product still belongs to that category
     */
    boolean existsByCategoryId(Long categoryId);
}
