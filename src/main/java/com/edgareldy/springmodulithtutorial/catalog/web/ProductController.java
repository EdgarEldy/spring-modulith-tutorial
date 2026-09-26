package com.edgareldy.springmodulithtutorial.catalog.web;

import com.edgareldy.springmodulithtutorial.catalog.ProductService;
import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import com.edgareldy.springmodulithtutorial.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point of the products: paginated listing (optionally by category) for users, creation for administrators.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * @param categoryId optional category filter
     * @param page       zero-based page index
     * @param size       page size, between 1 and 100
     * @return one page of products, ordered by identifier
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
        return ApiResponse.success(
                PageResponse.from(productService.findAll(categoryId, pageable), ProductResponse::from),
                "Products retrieved");
    }

    /**
     * @param request the product to create
     * @return the created product, with a 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = ProductResponse.from(
                productService.create(request.name(), request.unitPrice(), request.categoryId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Product created"));
    }
}
