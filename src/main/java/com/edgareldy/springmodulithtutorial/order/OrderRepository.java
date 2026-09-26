package com.edgareldy.springmodulithtutorial.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of orders.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
