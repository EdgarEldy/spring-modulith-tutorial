package com.edgareldy.springmodulithtutorial.catalog.web;

import com.edgareldy.springmodulithtutorial.catalog.Category;

/**
 * Category as returned by the HTTP API.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id   the category identifier
 * @param name the category name
 */
public record CategoryResponse(
        Long id,
        String name
) {

    /**
     * @param category the entity
     * @return its HTTP representation
     */
    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
