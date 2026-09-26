/**
 * Public API of the catalog module: the only package other modules may depend on.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @NamedInterface turns this sub-package into an explicitly exposed part of the catalog module, under
// the name "api". Sub-packages that are not named interfaces (impl, web) stay internal, but the public
// types of the base package (the entities and repositories used by impl) still form the module's
// unnamed interface. A consumer such as order therefore declares allowedDependencies = "catalog :: api":
// only then does ApplicationModules.verify() fail when it reaches past CatalogApi and ProductSummary.
@NamedInterface("api")
package com.edgareldy.springmodulithtutorial.catalog.api;

import org.springframework.modulith.NamedInterface;
