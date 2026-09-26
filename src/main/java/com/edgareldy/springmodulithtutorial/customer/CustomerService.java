package com.edgareldy.springmodulithtutorial.customer;

/**
 * Use cases of the customer module called by its own web layer: create a customer and read one back.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CustomerService {

    /**
     * Creates a customer after checking that no other customer uses the same email.
     *
     * @param request the validated request
     * @return the created customer
     * @throws com.edgareldy.springmodulithtutorial.common.BusinessRuleException if the email is already used
     */
    CustomerResponse create(CreateCustomerRequest request);

    /**
     * @param id the customer identifier
     * @return the customer
     * @throws com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException if no customer has this id
     */
    CustomerResponse findById(Long id);
}
