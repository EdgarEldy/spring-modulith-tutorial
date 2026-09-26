package com.edgareldy.springmodulithtutorial.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A placed order: one product, one customer, a quantity and the total computed when it was placed.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// ORDER is a reserved word of the query language, so the entity gets its own JPA name while the table
// keeps the name of the specification. customerId and productId are plain identifiers, not @ManyToOne
// associations: the customer and the product belong to other modules, which order only reaches through
// CustomerApi and CatalogApi, never through a join. Public in Java only because the service
// implementation lives in impl/; other modules are kept away from it by their allowedDependencies.
@Entity(name = "PlacedOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    // A snapshot of quantity * unit price taken when the order is placed. It is never recalculated, so a
    // later price change in the catalog cannot alter the total of an order already placed.
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    protected Order() {
    }

    public Order(Long customerId, Long productId, int quantity, BigDecimal total) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
