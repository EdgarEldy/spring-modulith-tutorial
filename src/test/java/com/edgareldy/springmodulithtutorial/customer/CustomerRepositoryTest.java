package com.edgareldy.springmodulithtutorial.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks {@link CustomerRepository} and the {@link Customer} mapping against the real V1 schema in PostgreSQL 16.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// No @DataJpaTest here: its starter is not on the classpath, and a JPA slice would start a context
// of its own (a second container). Reusing the exact annotations of the other integration tests lets
// Spring's context cache share one context and one PostgreSQL container. @Transactional rolls every
// test back, so the rows written here never leak into other classes.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void _01_ShouldPersistAndReloadEveryColumn_WhenACustomerIsSaved() {
        String email = uniqueEmail();
        Customer saved = customerRepository.saveAndFlush(
                new Customer("Jane", "Doe", "+250788123456", email, "1 Main Street"));

        assertThat(saved.getId()).isNotNull();
        assertThat(customerRepository.findById(saved.getId())).hasValueSatisfying(found -> {
            assertThat(found.getFirstName()).isEqualTo("Jane");
            assertThat(found.getLastName()).isEqualTo("Doe");
            assertThat(found.getTelephone()).isEqualTo("+250788123456");
            assertThat(found.getEmail()).isEqualTo(email);
            assertThat(found.getAddress()).isEqualTo("1 Main Street");
        });
    }

    @Test
    void _02_ShouldReportTheEmailAsUsed_WhenACustomerHasIt() {
        String email = uniqueEmail();
        customerRepository.saveAndFlush(new Customer("Jane", "Doe", "+250788123456", email, "1 Main Street"));

        assertThat(customerRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void _03_ShouldReportTheEmailAsFree_WhenNoCustomerHasIt() {
        assertThat(customerRepository.existsByEmail(uniqueEmail())).isFalse();
    }

    @Test
    void _04_ShouldThrowDataIntegrityViolation_WhenTwoCustomersShareAnEmail() {
        String email = uniqueEmail();
        customerRepository.saveAndFlush(new Customer("Jane", "Doe", "+250788123456", email, "1 Main Street"));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(
                new Customer("John", "Doe", "+250788654321", email, "2 Main Street")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String uniqueEmail() {
        return "repo-" + UUID.randomUUID() + "@example.com";
    }
}
