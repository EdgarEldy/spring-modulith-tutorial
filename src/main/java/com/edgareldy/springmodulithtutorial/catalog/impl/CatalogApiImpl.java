package com.edgareldy.springmodulithtutorial.catalog.impl;

import com.edgareldy.springmodulithtutorial.catalog.Product;
import com.edgareldy.springmodulithtutorial.catalog.ProductRepository;
import com.edgareldy.springmodulithtutorial.catalog.api.CatalogApi;
import com.edgareldy.springmodulithtutorial.catalog.api.ProductSummary;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-process implementation of {@link CatalogApi}, mapping the internal {@link Product} entity to a {@link ProductSummary}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Callers inject CatalogApi and never see this class. Extracting catalog into its own service later
// would mean replacing this bean by an HTTP or messaging client implementing the same interface.
@Service
@Transactional(readOnly = true)
class CatalogApiImpl implements CatalogApi {

    private final ProductRepository productRepository;

    CatalogApiImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductSummary> findProduct(Long id) {
        return productRepository.findById(id).map(CatalogApiImpl::toSummary);
    }

    @Override
    public boolean productExists(Long id) {
        return productRepository.existsById(id);
    }

    private static ProductSummary toSummary(Product product) {
        return new ProductSummary(product.getId(), product.getName(), product.getUnitPrice(),
                product.getCategory().getId());
    }
}
