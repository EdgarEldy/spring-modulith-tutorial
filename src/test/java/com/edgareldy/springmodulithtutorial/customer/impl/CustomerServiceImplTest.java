package com.edgareldy.springmodulithtutorial.customer.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import com.edgareldy.springmodulithtutorial.customer.CreateCustomerRequest;
import com.edgareldy.springmodulithtutorial.customer.Customer;
import com.edgareldy.springmodulithtutorial.customer.CustomerRepository;
import com.edgareldy.springmodulithtutorial.customer.CustomerResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of {@link CustomerServiceImpl}: email normalization, the unique email rule and lookups, with a mocked repository.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same package as the package-private implementation, so the test can instantiate it directly.
// No Spring context: the repository is a Mockito mock and the business rules are tested alone.
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void _01_ShouldSaveANormalizedCustomer_WhenTheEmailIsNotUsedYet() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "  Jane ", " Doe  ", " +250 788 123 456 ", "  Jane.Doe@Example.COM ", " 1 Main Street ");
        when(customerRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        CustomerResponse response = customerService.create(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getTelephone()).isEqualTo("+250 788 123 456");
        assertThat(saved.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(saved.getAddress()).isEqualTo("1 Main Street");
        assertThat(response).isEqualTo(new CustomerResponse(
                7L, "Jane", "Doe", "+250 788 123 456", "jane.doe@example.com", "1 Main Street"));
    }

    @Test
    void _02_ShouldThrowBusinessRuleAndSaveNothing_WhenTheEmailIsAlreadyUsed() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane", "Doe", "+250788123456", "JANE@example.com", "1 Main Street");
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("jane@example.com");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void _03_ShouldThrowBusinessRule_WhenTheUniqueConstraintFiresOnFlush() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane", "Doe", "+250788123456", "jane@example.com", "1 Main Street");
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("jane@example.com");
    }

    @Test
    void _04_ShouldReturnTheCustomer_WhenTheIdExists() {
        Customer customer = new Customer("Jane", "Doe", "+250788123456", "jane@example.com", "1 Main Street");
        ReflectionTestUtils.setField(customer, "id", 3L);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.findById(3L);

        assertThat(response).isEqualTo(new CustomerResponse(
                3L, "Jane", "Doe", "+250788123456", "jane@example.com", "1 Main Street"));
    }

    @Test
    void _05_ShouldThrowResourceNotFound_WhenTheIdIsUnknown() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }
}
