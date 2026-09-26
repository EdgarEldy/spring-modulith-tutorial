package com.edgareldy.springmodulithtutorial.customer.web;

import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import com.edgareldy.springmodulithtutorial.customer.CreateCustomerRequest;
import com.edgareldy.springmodulithtutorial.customer.CustomerResponse;
import com.edgareldy.springmodulithtutorial.customer.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints of the customer module: read a customer ({@code USER}) and create one ({@code ADMIN}).
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * @param id the customer identifier
     * @return 200 with the customer, 404 if it does not exist
     */
    // @PreAuthorize is evaluated before the method runs (method security is enabled once in common).
    // Roles are checked literally, without a hierarchy: an ADMIN token without the USER role is
    // refused here with a 403, exactly as the endpoint table of the README states.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    ApiResponse<CustomerResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(customerService.findById(id), "Customer found");
    }

    /**
     * @param request the validated creation request
     * @return 201 with the created customer and its location, 422 if the email is already used
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id()))
                .body(ApiResponse.success(created, "Customer created"));
    }
}
