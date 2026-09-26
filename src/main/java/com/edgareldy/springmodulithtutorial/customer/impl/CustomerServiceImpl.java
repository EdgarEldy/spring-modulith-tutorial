package com.edgareldy.springmodulithtutorial.customer.impl;

import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import com.edgareldy.springmodulithtutorial.customer.CreateCustomerRequest;
import com.edgareldy.springmodulithtutorial.customer.Customer;
import com.edgareldy.springmodulithtutorial.customer.CustomerRepository;
import com.edgareldy.springmodulithtutorial.customer.CustomerResponse;
import com.edgareldy.springmodulithtutorial.customer.CustomerService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CustomerService}: enforces the unique email rule and maps entities to responses.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Service
@Transactional(readOnly = true)
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String email = normalizeEmail(request.email());
        if (customerRepository.existsByEmail(email)) {
            throw duplicateEmail(email);
        }
        Customer customer = new Customer(
                request.firstName().strip(),
                request.lastName().strip(),
                request.telephone().strip(),
                email,
                request.address().strip());
        try {
            // Flushing here makes the unique constraint fire inside this method, so two concurrent
            // requests racing past existsByEmail still end in a 422 rather than a 500.
            return CustomerResponse.from(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateEmail(email);
        }
    }

    @Override
    public CustomerResponse findById(Long id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    // Emails are compared case-insensitively by storing them in lower case: the unique constraint of
    // the customers table then also rejects "Jane@x.io" when "jane@x.io" already exists.
    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static BusinessRuleException duplicateEmail(String email) {
        return new BusinessRuleException("A customer with email " + email + " already exists");
    }
}
