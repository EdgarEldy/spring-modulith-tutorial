/**
 * Public API of the auth module: the only package other modules are allowed to depend on.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// @NamedInterface turns this sub-package into an explicitly exposed part of the module. Every other
// type of auth, whatever its Java modifier, stays internal: ApplicationModules.verify() fails as soon
// as another module references it. Other modules therefore see AuthApi and nothing else, which is also
// what would survive unchanged if auth were extracted into its own service later.
@NamedInterface("api")
package com.edgareldy.springmodulithtutorial.auth.api;

import org.springframework.modulith.NamedInterface;
