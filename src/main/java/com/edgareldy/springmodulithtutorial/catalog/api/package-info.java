/**
 * Public API of the catalog module: the only package other modules may depend on.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @NamedInterface turns this sub-package into an explicitly exposed part of the catalog module.
// Once a module declares a named interface, Spring Modulith treats every other package of it
// (entities, repositories, services, impl, web) as internal, and ApplicationModules.verify() fails
// if the order module, or any other one, reaches past CatalogApi and ProductSummary.
@NamedInterface("api")
package com.edgareldy.springmodulithtutorial.catalog.api;

import org.springframework.modulith.NamedInterface;
