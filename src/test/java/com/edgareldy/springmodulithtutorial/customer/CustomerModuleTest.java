package com.edgareldy.springmodulithtutorial.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.common.GlobalExceptionHandler;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerApi;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerSummary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Module test of customer: boots the customer module alone and checks its public API against a real database.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @ApplicationModuleTest bootstraps only the module of the test's package (default mode STANDALONE):
// component scanning, entity scanning and Spring Data repositories are limited to the customer package
// and its sub-packages, so no bean of catalog, order, auth or common is created. Spring Boot's
// auto-configuration still applies (DataSource, JPA, Flyway), so the module needs a real database,
// hence the Testcontainers import. customer depends on no other module, so STANDALONE is enough.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class CustomerModuleTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerApi customerApi;

    @Autowired
    private ApplicationContext context;

    @Test
    void _01_ShouldExposeTheSummaryThroughTheApi_WhenACustomerWasCreated() {
        String email = uniqueEmail();
        CustomerResponse created = customerService.create(
                new CreateCustomerRequest("Jane", "Doe", "+250788123456", email, "1 Main Street"));

        assertThat(customerApi.findCustomer(created.id()))
                .contains(new CustomerSummary(created.id(), "Jane", "Doe", email));
        assertThat(customerApi.customerExists(created.id())).isTrue();
    }

    @Test
    void _02_ShouldReturnEmptyAndFalse_WhenTheIdIsUnknown() {
        assertThat(customerApi.findCustomer(Long.MAX_VALUE)).isEmpty();
        assertThat(customerApi.customerExists(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void _03_ShouldNotBootOtherModules_WhenOnlyCustomerIsUnderTest() {
        // GlobalExceptionHandler belongs to common: its absence proves the context is limited to customer.
        assertThat(context.getBeanNamesForType(GlobalExceptionHandler.class)).isEmpty();
    }

    private static String uniqueEmail() {
        return "module-" + UUID.randomUUID() + "@example.com";
    }
}
