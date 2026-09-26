/**
 * Order module: places orders against the catalog and customer public APIs and publishes
 * an event once an order is persisted.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Spring Modulith treats the public types of another module's base package (its unnamed interface) as
// usable API, so a public entity such as catalog.Product would not be reported on its own. Declaring
// allowedDependencies narrows what order may use to exactly these targets: the "api" named interfaces of
// catalog and customer, plus the open common module. verify() then fails as soon as order touches
// anything else, such as an entity, a repository or a service of those modules.
@ApplicationModule(allowedDependencies = {"catalog :: api", "customer :: api", "common"})
package com.edgareldy.springmodulithtutorial.order;

import org.springframework.modulith.ApplicationModule;
