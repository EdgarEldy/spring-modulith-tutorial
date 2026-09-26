package com.edgareldy.springmodulithtutorial.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks the catalog repositories and entity mappings against the real PostgreSQL schema of V1.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as the other integration tests so the cached context and its container are
// reused. @Transactional rolls every test back, so the rows written here never leak into other classes.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogRepositoriesTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void _01_ShouldPersistAndReloadTheExactPrice_WhenAProductIsSaved() {
        Category category = categoryRepository.save(new Category(uniqueName("Books")));
        Product product = productRepository.save(new Product(category, "Clean Code", new BigDecimal("39.90")));
        entityManager.flush();
        entityManager.clear();

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        assertThat(reloaded.getName()).isEqualTo("Clean Code");
        assertThat(reloaded.getUnitPrice()).isEqualByComparingTo("39.90");
        assertThat(reloaded.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void _02_ShouldReturnOnlyTheProductsOfTheCategory_WhenFilteringByCategory() {
        Category books = categoryRepository.save(new Category(uniqueName("Books")));
        Category games = categoryRepository.save(new Category(uniqueName("Games")));
        productRepository.save(new Product(books, "Clean Code", BigDecimal.TEN));
        productRepository.save(new Product(books, "Refactoring", BigDecimal.TEN));
        productRepository.save(new Product(games, "Chess", BigDecimal.ONE));

        Page<Product> page = productRepository.findByCategoryId(books.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Product::getName).containsExactlyInAnyOrder("Clean Code", "Refactoring");
    }

    @Test
    void _03_ShouldTellWhetherACategoryHasProducts_WhenCheckingByCategoryId() {
        Category books = categoryRepository.save(new Category(uniqueName("Books")));
        Category empty = categoryRepository.save(new Category(uniqueName("Empty")));
        productRepository.save(new Product(books, "Clean Code", BigDecimal.TEN));

        assertThat(productRepository.existsByCategoryId(books.getId())).isTrue();
        assertThat(productRepository.existsByCategoryId(empty.getId())).isFalse();
    }

    @Test
    void _04_ShouldFindTheCategoryName_WhenItIsAlreadyUsed() {
        String name = uniqueName("Books");
        categoryRepository.save(new Category(name));

        assertThat(categoryRepository.existsByName(name)).isTrue();
        assertThat(categoryRepository.existsByName(uniqueName("Unknown"))).isFalse();
    }

    @Test
    void _05_ShouldBeRejectedByTheSchema_WhenACategoryNameIsDuplicated() {
        String name = uniqueName("Books");
        categoryRepository.saveAndFlush(new Category(name));

        // The UNIQUE constraint of V1 is the last line of defence behind CategoryService's own check.
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category(name)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
