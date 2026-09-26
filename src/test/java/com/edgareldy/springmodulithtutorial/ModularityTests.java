package com.edgareldy.springmodulithtutorial;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architectural test: fails the build as soon as one module reaches into another module's internals,
 * then documents the verified module model as PlantUML diagrams and module canvases.
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

    @Test
    void _02_ShouldWriteDiagramsAndCanvases_WhenDocumentingTheVerifiedModules() throws IOException {
        // Documenter renders the same ApplicationModules model that verify() just checked, so the
        // diagrams and canvases are derived from the code and cannot drift from it. Output goes to
        // target/spring-modulith-docs; the "docs" Maven profile copies it to docs/ for the repository.
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();

        Path docs = Path.of("target", "spring-modulith-docs");
        assertThat(docs.resolve("components.puml")).exists();
        for (String module : List.of("auth", "catalog", "customer", "order", "notification")) {
            assertThat(docs.resolve("module-" + module + ".puml")).exists();
            assertThat(docs.resolve("module-" + module + ".adoc")).exists();
        }
        // Canvases abbreviate packages (c.e.s.o.OrderPlacedEvent). The order canvas lists the event under
        // "Published events" because the record carries jMolecules @DomainEvent, which is how Modulith
        // 2.0.7 recognizes published events; it also shows that order references only the other modules' api/.
        assertThat(Files.readString(docs.resolve("module-order.adoc"), StandardCharsets.UTF_8))
                .contains("Published events", "c.e.s.o.OrderPlacedEvent",
                        "c.e.s.c.api.CatalogApi", "c.e.s.c.api.CustomerApi");
        assertThat(Files.readString(docs.resolve("module-notification.adoc"), StandardCharsets.UTF_8))
                .contains("OrderPlacedEventListener", "Events listened to", "c.e.s.o.OrderPlacedEvent");
    }
}
