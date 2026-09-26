package com.edgareldy.springmodulithtutorial.order.web;

import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import com.edgareldy.springmodulithtutorial.common.PageResponse;
import com.edgareldy.springmodulithtutorial.order.CreateOrderRequest;
import com.edgareldy.springmodulithtutorial.order.OrderResponse;
import com.edgareldy.springmodulithtutorial.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints of the order module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * @param id the order identifier
     * @return the order
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    ApiResponse<OrderResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(orderService.findById(id), "Order found");
    }

    /**
     * @param request the order to place
     * @return the placed order, with a 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id()))
                .body(ApiResponse.success(created, "Order placed"));
    }

    /**
     * @param page zero-based page index
     * @param size page size, between 1 and 100
     * @return one page of orders, ordered by identifier
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                PageResponse.from(orderService.findAll(PageRequest.of(page, size, Sort.by("id")))),
                "Orders retrieved");
    }
}
