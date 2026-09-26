package com.edgareldy.springmodulithtutorial.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A customer who places orders, mapped onto the {@code customers} table created by V1.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Public in Java only because the service implementation lives in the impl/ sub-package. Other modules
// must read customers through customer.api.CustomerApi instead: a public type of the base package is
// part of the module's unnamed interface, so that rule is enforced by verify() on the consumer side,
// once it declares allowedDependencies = "customer :: api". There is deliberately no user_id column:
// linking a customer to an auth user would be a foreign key across a module boundary.
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "telephone", nullable = false, length = 30)
    private String telephone;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "address", nullable = false)
    private String address;

    /**
     * Required by JPA, not meant to be called by application code.
     */
    protected Customer() {
    }

    /**
     * @param firstName the first name
     * @param lastName  the last name
     * @param telephone the telephone number
     * @param email     the email address, unique among customers
     * @param address   the postal address
     */
    public Customer(String firstName, String lastName, String telephone, String email, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.telephone = telephone;
        this.email = email;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }
}
