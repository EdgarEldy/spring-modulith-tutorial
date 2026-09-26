package com.edgareldy.springmodulithtutorial.catalog.impl;

import com.edgareldy.springmodulithtutorial.catalog.Category;
import com.edgareldy.springmodulithtutorial.catalog.CategoryRepository;
import com.edgareldy.springmodulithtutorial.catalog.CategoryService;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.common.BusinessRuleException;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CategoryService}, enforcing the unique name and the "no products left" deletion rule.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Package-private: component scanning instantiates it and callers only know the CategoryService
// interface, so nothing needs the implementation type itself.
@Service
@Transactional(readOnly = true)
class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public Category create(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessRuleException("A category named '" + name + "' already exists");
        }
        return categoryRepository.save(new Category(name));
    }

    @Override
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        // The foreign key of products.category_id would reject this delete anyway, but as a 500 with
        // a SQL error. Checking first turns it into an explicit business rule answered with a 422.
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Category " + id + " still has products and cannot be deleted");
        }
        categoryRepository.delete(category);
    }
}
