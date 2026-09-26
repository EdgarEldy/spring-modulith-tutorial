/**
 * Shared building blocks every module may use freely: the {@code ApiResponse} envelope, the
 * {@code PageResponse} page payload, the base exceptions and the global exception handler.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// A regular application module only exposes its named interfaces. Declaring this one OPEN tells
// Spring Modulith that all of its types are public API, so any module may depend on them without
// verify() reporting a violation. It stays acceptable only because common carries no business
// logic and no state of its own.
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.edgareldy.springmodulithtutorial.common;

import org.springframework.modulith.ApplicationModule;
