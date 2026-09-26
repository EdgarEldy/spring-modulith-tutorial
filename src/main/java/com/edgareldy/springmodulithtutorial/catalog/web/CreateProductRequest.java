package com.edgareldy.springmodulithtutorial.catalog.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body of {@code POST /api/v1/products}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param name       the product name, at most 150 characters like the {@code product_name} column
 * @param unitPrice  a strictly positive price fitting {@code NUMERIC(12, 2)}
 * @param categoryId the existing category the product belongs to
 */
public record CreateProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @NotNull @Positive Long categoryId
) {
}
