package com.edgareldy.springmodulithtutorial.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.edgareldy.springmodulithtutorial.catalog.api.CatalogApi;
import com.edgareldy.springmodulithtutorial.catalog.api.ProductSummary;
import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Module test of catalog: its services and named interface wired together, alone, on a real database.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @ApplicationModuleTest boots a Spring Boot context restricted to the module of the test's package:
// in the default STANDALONE bootstrap mode, only the beans, entities and repositories of catalog are
// scanned (plus Spring Boot's auto-configuration), none of the other modules. It proves catalog works
// on its own, which is what its lack of dependencies on other modules promises. Its repositories are
// real JPA repositories validated against the Flyway schema, hence the PostgreSQL container.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class CatalogModuleTest {

    @Autowired
    private CatalogApi catalogApi;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void _01_ShouldReturnTheProductSummary_WhenTheProductWasCreatedThroughTheServices() {
        Long categoryId = categoryService.create(uniqueName("Books")).getId();
        Long productId = productService.create("Clean Code", new BigDecimal("39.90"), categoryId).getId();

        ProductSummary summary = catalogApi.findProduct(productId).orElseThrow();

        assertThat(summary.id()).isEqualTo(productId);
        assertThat(summary.name()).isEqualTo("Clean Code");
        assertThat(summary.unitPrice()).isEqualByComparingTo("39.90");
        assertThat(summary.categoryId()).isEqualTo(categoryId);
        assertThat(catalogApi.productExists(productId)).isTrue();
    }

    @Test
    void _02_ShouldReturnEmptyAndNotExist_WhenTheProductIdIsUnknown() {
        assertThat(catalogApi.findProduct(Long.MAX_VALUE)).isEmpty();
        assertThat(catalogApi.productExists(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void _03_ShouldRefuseTheDeletionAndKeepTheCategory_WhenItStillHasProducts() {
        Long categoryId = categoryService.create(uniqueName("Games")).getId();
        productService.create("Chess", new BigDecimal("15.00"), categoryId);

        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("still has products");
        assertThat(categoryRepository.existsById(categoryId)).isTrue();
    }

    @Test
    void _04_ShouldRemoveTheCategory_WhenItHasNoProducts() {
        Long categoryId = categoryService.create(uniqueName("Empty")).getId();

        categoryService.delete(categoryId);

        assertThat(categoryRepository.existsById(categoryId)).isFalse();
        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
