package com.edgareldy.springmodulithtutorial.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A product category, mapped onto the {@code categories} table created by the V1 migration.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Public in Java only because the service implementations in catalog.impl use it. For Spring
// Modulith it stays internal to catalog: it is outside the api named interface, so verify() fails
// as soon as another module imports it.
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Required by JPA, not meant to be called by application code.
     */
    protected Category() {
    }

    /**
     * @param name the category name, unique across the catalog
     */
    public Category(String name) {
        this.name = name;
    }

    /**
     * @return the database identifier, {@code null} until persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the category name
     */
    public String getName() {
        return name;
    }
}
