package com.edgareldy.springmodulithtutorial.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/categories}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param name the category name, at most 100 characters like the {@code category_name} column
 */
public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name
) {
}
