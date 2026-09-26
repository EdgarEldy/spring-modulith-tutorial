/**
 * Public API of the customer module: the only package other modules may depend on.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Spring Modulith exposes the public types of a module's base package (its unnamed interface) and
// hides every sub-package. @NamedInterface publishes this sub-package under the name "api", so other
// modules (order, later) can reach CustomerApi and CustomerSummary. A consumer that declares
// @ApplicationModule(allowedDependencies = "customer :: api") is then limited to exactly these types:
// ApplicationModules.verify() fails as soon as it touches the entity, the repository or the service.
@NamedInterface("api")
package com.edgareldy.springmodulithtutorial.customer.api;

import org.springframework.modulith.NamedInterface;
