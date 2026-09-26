package com.edgareldy.springmodulithtutorial;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Architectural test: fails the build as soon as one module reaches into another module's internals.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
class ModularityTests {

    // ApplicationModules.of(...) analyses the compiled classes (with ArchUnit) starting from the
    // application class and builds the module model: one module per direct sub-package, each with
    // its named interfaces and its dependencies on other modules. No Spring context is started, so
    // this test runs in about a second and needs no database.
    private final ApplicationModules modules = ApplicationModules.of(SpringModulithTutorialApplication.class);

    @Test
    void _01_ShouldReportNoViolation_WhenVerifyingTheModuleBoundaries() {
        // verify() throws with a readable report if a module depends on a type another module does
        // not expose, or if modules depend on each other in a cycle. This is the logical boundary
        // enforcement that replaces what separate Maven modules would get from the compiler.
        modules.verify();
    }
}
