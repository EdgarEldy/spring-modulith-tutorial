package com.edgareldy.springmodulithtutorial.catalog.web;

import com.edgareldy.springmodulithtutorial.catalog.CategoryService;
import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import com.edgareldy.springmodulithtutorial.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * HTTP entry point of the categories: paginated listing for users, creation for administrators.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Deliberately no DELETE endpoint: the "a category with products cannot be deleted" rule lives in
// CategoryService.delete and is covered by its tests, but the API does not expose deletion.
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

    private final CategoryService categoryService;

    CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * @param page zero-based page index
     * @param size page size, between 1 and 100
     * @return one page of categories, ordered by identifier
     */
    // @PreAuthorize is evaluated by the method security proxy (enabled once in common) before the
    // method runs: an authenticated caller without the role gets an AccessDeniedException, which the
    // GlobalExceptionHandler turns into a 403. hasRole('USER') checks the ROLE_USER authority only,
    // there is no role hierarchy, so an ADMIN is not implicitly a USER.
    // The constraints on the request parameters are checked by Spring MVC's built-in method
    // validation (a 400 through the HandlerMethodValidationException). No class-level @Validated here:
    // it would switch to the AOP flavour, whose ConstraintViolationException would become a 500.
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<CategoryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
        return ApiResponse.success(
                PageResponse.from(categoryService.findAll(pageable), CategoryResponse::from),
                "Categories retrieved");
    }

    /**
     * @param request the category to create
     * @return the created category, with a 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = CategoryResponse.from(categoryService.create(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Category created"));
    }
}
