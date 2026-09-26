package com.edgareldy.springmodulithtutorial.catalog.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.catalog.Category;
import com.edgareldy.springmodulithtutorial.catalog.CategoryRepository;
import com.edgareldy.springmodulithtutorial.catalog.Product;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests of {@link ProductServiceImpl}, with mocked repositories.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void _01_ShouldSaveTheProductInItsCategory_WhenTheCategoryExists() {
        Category category = new Category("Books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create("Clean Code", new BigDecimal("39.90"), 1L);

        assertThat(created.getName()).isEqualTo("Clean Code");
        assertThat(created.getUnitPrice()).isEqualByComparingTo("39.90");
        assertThat(created.getCategory()).isSameAs(category);
    }

    @Test
    void _02_ShouldThrowNotFound_WhenTheCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create("Clean Code", BigDecimal.TEN, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with id 99 not found");
        verify(productRepository, never()).save(any());
    }

    @Test
    void _03_ShouldListEveryProduct_WhenNoCategoryIsGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findAll(pageable)).thenReturn(page);

        assertThat(productService.findAll(null, pageable)).isSameAs(page);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void _04_ShouldFilterOnTheCategory_WhenACategoryIsGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(page);

        assertThat(productService.findAll(1L, pageable)).isSameAs(page);
        verify(productRepository, never()).findAll(pageable);
    }
}
