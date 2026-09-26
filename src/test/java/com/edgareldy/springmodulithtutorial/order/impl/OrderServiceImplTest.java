package com.edgareldy.springmodulithtutorial.order.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.catalog.api.CatalogApi;
import com.edgareldy.springmodulithtutorial.catalog.api.ProductSummary;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import com.edgareldy.springmodulithtutorial.customer.api.CustomerApi;
import com.edgareldy.springmodulithtutorial.order.CreateOrderRequest;
import com.edgareldy.springmodulithtutorial.order.Order;
import com.edgareldy.springmodulithtutorial.order.OrderPlacedEvent;
import com.edgareldy.springmodulithtutorial.order.OrderRepository;
import com.edgareldy.springmodulithtutorial.order.OrderResponse;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of {@link OrderServiceImpl}: total snapshot, persisted values, published event and the 404 rules.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// The collaborators from other modules are mocked through their named-interface types only, exactly as the
// implementation sees them. No Spring context: the event publisher is a plain mock, so the test checks what
// is published, not how Spring Modulith delivers it.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogApi catalogApi;

    @Mock
    private CustomerApi customerApi;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void _01_ShouldSaveTheTotalSnapshotAndPublishTheEvent_WhenCustomerAndProductExist() {
        when(customerApi.customerExists(5L)).thenReturn(true);
        when(catalogApi.findProduct(8L))
                .thenReturn(Optional.of(new ProductSummary(8L, "Clean Code", new BigDecimal("19.99"), 2L)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });

        OrderResponse response = orderService.create(new CreateOrderRequest(5L, 8L, 3));

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getCustomerId()).isEqualTo(5L);
        assertThat(order.getValue().getProductId()).isEqualTo(8L);
        assertThat(order.getValue().getQuantity()).isEqualTo(3);
        assertThat(order.getValue().getTotal()).isEqualTo(new BigDecimal("59.97"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new OrderPlacedEvent(42L, 5L, 8L, 3, new BigDecimal("59.97")));
        assertThat(response).isEqualTo(new OrderResponse(42L, 5L, 8L, 3, new BigDecimal("59.97")));
    }

    @Test
    void _02_ShouldRoundTheTotalToTwoDecimals_WhenTheUnitPriceHasMoreDecimals() {
        when(customerApi.customerExists(5L)).thenReturn(true);
        when(catalogApi.findProduct(8L))
                .thenReturn(Optional.of(new ProductSummary(8L, "Pencil", new BigDecimal("0.125"), 2L)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(new CreateOrderRequest(5L, 8L, 3));

        // 0.125 x 3 = 0.375, rounded HALF_UP to the scale of the orders.total column.
        assertThat(response.total()).isEqualTo(new BigDecimal("0.38"));
    }

    @Test
    void _03_ShouldThrowNotFoundAndSaveOrPublishNothing_WhenTheCustomerIsUnknown() {
        when(customerApi.customerExists(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(99L, 8L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer with id 99 not found");
        verifyNoInteractions(catalogApi, orderRepository, events);
    }

    @Test
    void _04_ShouldThrowNotFoundAndSaveOrPublishNothing_WhenTheProductIsUnknown() {
        when(customerApi.customerExists(5L)).thenReturn(true);
        when(catalogApi.findProduct(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(5L, 77L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 77 not found");
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void _05_ShouldReturnTheOrder_WhenTheIdExists() {
        Order order = new Order(5L, 8L, 2, new BigDecimal("39.98"));
        ReflectionTestUtils.setField(order, "id", 3L);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThat(orderService.findById(3L))
                .isEqualTo(new OrderResponse(3L, 5L, 8L, 2, new BigDecimal("39.98")));
    }

    @Test
    void _06_ShouldThrowResourceNotFound_WhenTheOrderIdIsUnknown() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order with id 99 not found");
    }
}
