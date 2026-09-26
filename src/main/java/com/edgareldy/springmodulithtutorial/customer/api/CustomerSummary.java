package com.edgareldy.springmodulithtutorial.customer.api;

/**
 * What other modules may know about a customer: a read-only copy, never the JPA entity itself.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id        the customer identifier
 * @param firstName the first name
 * @param lastName  the last name
 * @param email     the email address
 */
public record CustomerSummary(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}
