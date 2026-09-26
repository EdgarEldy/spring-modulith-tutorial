package com.edgareldy.springmodulithtutorial.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks {@link OrderRepository} and the {@link Order} mapping against the real V1 schema in PostgreSQL 16.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as the other integration tests, so the cached context and its container are reused.
// @Transactional rolls each test back. The persistence context is cleared before reading, otherwise
// findById would return the very instance just saved instead of what PostgreSQL actually stored.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void _01_ShouldPersistAndReloadEveryColumn_WhenAnOrderIsSaved() {
        Long id = orderRepository.saveAndFlush(new Order(5L, 8L, 3, new BigDecimal("59.97"))).getId();
        entityManager.clear();

        assertThat(id).isNotNull();
        assertThat(orderRepository.findById(id)).hasValueSatisfying(found -> {
            assertThat(found.getCustomerId()).isEqualTo(5L);
            assertThat(found.getProductId()).isEqualTo(8L);
            assertThat(found.getQuantity()).isEqualTo(3);
            assertThat(found.getTotal()).isEqualTo(new BigDecimal("59.97"));
        });
    }

    @Test
    void _02_ShouldKeepTwoDecimalsWithoutLoss_WhenTheTotalIsTheLargestTheColumnAccepts() {
        // NUMERIC(14, 2): twelve integer digits and two decimals, stored exactly, never as a float.
        Long id = orderRepository.saveAndFlush(new Order(5L, 8L, 1, new BigDecimal("999999999999.99"))).getId();
        entityManager.clear();

        BigDecimal total = orderRepository.findById(id).orElseThrow().getTotal();
        assertThat(total).isEqualTo(new BigDecimal("999999999999.99"));
        assertThat(total.scale()).isEqualTo(2);
    }
}
