package com.edgareldy.springmodulithtutorial.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use cases on categories: creation, paginated listing and guarded deletion.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CategoryService {

    /**
     * @param name the name of the new category
     * @return the persisted category
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException if the name is already used
     */
    Category create(String name);

    /**
     * @param pageable the requested page
     * @return one page of categories
     */
    Page<Category> findAll(Pageable pageable);

    /**
     * Deletes a category, provided no product belongs to it anymore.
     *
     * @param id the category identifier
     * @throws com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException if the category does not exist
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException     if products still belong to it
     */
    void delete(Long id);
}
