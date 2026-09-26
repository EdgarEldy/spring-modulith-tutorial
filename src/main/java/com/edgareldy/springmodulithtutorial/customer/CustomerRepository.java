package com.edgareldy.springmodulithtutorial.customer;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link Customer}, internal to the customer module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * @param email the email address, already normalized to lower case
     * @return whether a customer already uses this email
     */
    boolean existsByEmail(String email);
}
