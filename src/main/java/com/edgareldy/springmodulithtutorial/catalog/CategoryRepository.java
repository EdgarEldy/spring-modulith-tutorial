package com.edgareldy.springmodulithtutorial.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link Category}, internal to the catalog module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * @param name a category name
     * @return whether a category already uses this exact name
     */
    boolean existsByName(String name);
}
