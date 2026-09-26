package com.edgareldy.springmodulithtutorial.customer.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.customer.Customer;
import com.edgareldy.springmodulithtutorial.customer.CustomerRepository;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerSummary;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of {@link CustomerApiImpl}: the entity is translated into a {@link CustomerSummary}, never exposed as is.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CustomerApiImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerApiImpl customerApi;

    @Test
    void _01_ShouldReturnTheSummary_WhenTheCustomerExists() {
        Customer customer = new Customer("Jane", "Doe", "+250788123456", "jane@example.com", "1 Main Street");
        ReflectionTestUtils.setField(customer, "id", 5L);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));

        assertThat(customerApi.findCustomer(5L))
                .contains(new CustomerSummary(5L, "Jane", "Doe", "jane@example.com"));
    }

    @Test
    void _02_ShouldReturnEmpty_WhenTheCustomerDoesNotExist() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(customerApi.findCustomer(404L)).isEmpty();
    }

    @Test
    void _03_ShouldReturnTrue_WhenTheCustomerExists() {
        when(customerRepository.existsById(5L)).thenReturn(true);

        assertThat(customerApi.customerExists(5L)).isTrue();
    }

    @Test
    void _04_ShouldReturnFalse_WhenTheCustomerDoesNotExist() {
        when(customerRepository.existsById(404L)).thenReturn(false);

        assertThat(customerApi.customerExists(404L)).isFalse();
    }
}
