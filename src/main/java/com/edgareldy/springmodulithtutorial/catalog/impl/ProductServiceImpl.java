package com.edgareldy.springmodulithtutorial.catalog.impl;

import com.edgareldy.springmodulithtutorial.catalog.Category;
import com.edgareldy.springmodulithtutorial.catalog.CategoryRepository;
import com.edgareldy.springmodulithtutorial.catalog.Product;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.catalog.ProductService;
import com.edgareldy.springmodulithtutorial.common.ResourceNotFoundException;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ProductService}: a product can only be created inside an existing category.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@Service
@Transactional(readOnly = true)
class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Product create(String name, BigDecimal unitPrice, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
        return productRepository.save(new Product(category, name, unitPrice));
    }

    @Override
    public Page<Product> findAll(Long categoryId, Pageable pageable) {
        return categoryId == null
                ? productRepository.findAll(pageable)
                : productRepository.findByCategoryId(categoryId, pageable);
    }
}
