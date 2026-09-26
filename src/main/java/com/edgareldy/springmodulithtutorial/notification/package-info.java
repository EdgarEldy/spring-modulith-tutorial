/**
 * Notification module: reacts to events published by other modules, without a message broker.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// notification only listens: the one type it may use from another module is OrderPlacedEvent, published
// by order as its "events" named interface. Any other dependency, on the Order entity or the service of
// order for instance, makes ApplicationModules.verify() fail.
@ApplicationModule(allowedDependencies = "order :: events")
package com.edgareldy.springmodulithtutorial.notification;

import org.springframework.modulith.ApplicationModule;
