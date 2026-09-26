package com.edgareldy.springmodulithtutorial.customer;

/**
 * Customer detail returned by the customer endpoints, so the entity itself never leaves the module.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param id        the customer identifier
 * @param firstName the first name
 * @param lastName  the last name
 * @param telephone the telephone number
 * @param email     the email address
 * @param address   the postal address
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String telephone,
        String email,
        String address
) {

    /**
     * @param customer the entity to copy
     * @return the response
     */
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getTelephone(),
                customer.getEmail(),
                customer.getAddress()
        );
    }
}
