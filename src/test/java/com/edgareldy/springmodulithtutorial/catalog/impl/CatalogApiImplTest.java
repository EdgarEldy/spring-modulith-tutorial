package com.edgareldy.springmodulithtutorial.catalog.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.catalog.Category;
import com.edgareldy.springmodulithtutorial.catalog.Product;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.catalog.api.ProductSummary;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of {@link CatalogApiImpl}: the entity never leaves the module, only a summary does.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CatalogApiImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CatalogApiImpl catalogApi;

    @Test
    void _01_ShouldReturnTheProductSummary_WhenTheProductExists() {
        Category category = new Category("Books");
        ReflectionTestUtils.setField(category, "id", 3L);
        Product product = new Product(category, "Clean Code", new BigDecimal("39.90"));
        ReflectionTestUtils.setField(product, "id", 7L);
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        Optional<ProductSummary> summary = catalogApi.findProduct(7L);

        assertThat(summary).contains(new ProductSummary(7L, "Clean Code", new BigDecimal("39.90"), 3L));
    }

    @Test
    void _02_ShouldReturnEmpty_WhenTheProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(catalogApi.findProduct(99L)).isEmpty();
    }

    @Test
    void _03_ShouldTellWhetherTheProductExists_WhenAskedByIdentifier() {
        when(productRepository.existsById(7L)).thenReturn(true);
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThat(catalogApi.productExists(7L)).isTrue();
        assertThat(catalogApi.productExists(99L)).isFalse();
    }
}
