package com.edgareldy.springmodulithtutorial.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A product of the catalog, belonging to exactly one {@link Category}, mapped onto the {@code products} table.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Both ends of this association live in the catalog module, so a real JPA relation (and the
// products.category_id foreign key of V1) is fine here. Other modules never see this entity: the
// order module will only get a ProductSummary through CatalogApi.
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false, length = 150)
    private String name;

    // NUMERIC(12, 2) in V1: BigDecimal keeps the exact amount, where a double would round prices.
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Required by JPA, not meant to be called by application code.
     */
    protected Product() {
    }

    /**
     * @param category  the owning category
     * @param name      the product name
     * @param unitPrice the unit price, positive
     */
    public Product(Category category, String name, BigDecimal unitPrice) {
        this.category = category;
        this.name = name;
        this.unitPrice = unitPrice;
    }

    /**
     * @return the database identifier, {@code null} until persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the owning category (a lazy proxy when loaded from the database)
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the unit price
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
