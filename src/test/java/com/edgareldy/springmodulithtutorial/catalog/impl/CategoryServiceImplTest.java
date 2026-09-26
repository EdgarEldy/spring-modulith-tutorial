package com.edgareldy.springmodulithtutorial.catalog.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmodulithtutorial.catalog.Category;
import com.edgareldy.springmodulithtutorial.catalog.CategoryRepository;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
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
 * Unit tests of {@link CategoryServiceImpl}, with mocked repositories.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void _01_ShouldSaveTheCategory_WhenTheNameIsFree() {
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create("Books");

        assertThat(created.getName()).isEqualTo("Books");
        verify(categoryRepository).save(created);
    }

    @Test
    void _02_ShouldRejectTheCategory_WhenTheNameIsAlreadyUsed() {
        when(categoryRepository.existsByName("Books")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create("Books"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Books");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void _03_ShouldReturnTheRepositoryPage_WhenListingCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(new Category("Books")), pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        assertThat(categoryService.findAll(pageable)).isSameAs(page);
    }

    @Test
    void _04_ShouldDeleteTheCategory_WhenItHasNoProducts() {
        Category category = new Category("Books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void _05_ShouldRefuseTheDeletion_WhenTheCategoryStillHasProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Books")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("still has products");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void _06_ShouldThrowNotFound_WhenDeletingAnUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with id 99 not found");
        verify(categoryRepository, never()).delete(any());
    }
}
