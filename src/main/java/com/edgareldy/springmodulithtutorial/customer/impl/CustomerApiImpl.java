package com.edgareldy.springmodulithtutorial.customer.impl;

import com.edgareldy.springmodulithtutorial.customer.Customer;
import com.edgareldy.springmodulithtutorial.customer.CustomerRepository;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerApi;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerSummary;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link CustomerApi} inside the module, translating entities into {@link CustomerSummary} records.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Other modules inject the CustomerApi interface and never see this class. If customer were extracted
// into its own service one day, an HTTP client implementing the same interface would replace it
// without any change on the calling side.
@Service
@Transactional(readOnly = true)
class CustomerApiImpl implements CustomerApi {

    private final CustomerRepository customerRepository;

    CustomerApiImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<CustomerSummary> findCustomer(Long id) {
        return customerRepository.findById(id).map(CustomerApiImpl::toSummary);
    }

    @Override
    public boolean customerExists(Long id) {
        return customerRepository.existsById(id);
    }

    private static CustomerSummary toSummary(Customer customer) {
        return new CustomerSummary(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail());
    }
}
