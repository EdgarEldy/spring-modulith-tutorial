package com.edgareldy.springmodulithtutorial.customer.api;

import java.util.Optional;

/**
 * The only door into the customer module for other modules, e.g. for order to check that a customer exists.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CustomerApi {

    /**
     * @param id the customer identifier
     * @return the customer summary, or empty if no customer has this id
     */
    Optional<CustomerSummary> findCustomer(Long id);

    /**
     * @param id the customer identifier
     * @return whether a customer has this id
     */
    boolean customerExists(Long id);
}
