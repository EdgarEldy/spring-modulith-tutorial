package com.edgareldy.springmodulithtutorial.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Checks the Actuator health endpoints, including the database indicator, without credentials.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthEndpointsTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void _01_ShouldReportUpWithDatabaseComponent_WhenTheDatabaseIsReachable() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.status").isEqualTo("UP");
                    json.assertThat().extractingPath("$.components.db.status").isEqualTo("UP");
                });
    }

    @Test
    void _02_ShouldIncludeTheDatabase_WhenCheckingReadiness() {
        assertThat(mvc.get().uri("/actuator/health/readiness"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.status").isEqualTo("UP");
                    json.assertThat().extractingPath("$.components.db.status").isEqualTo("UP");
                    json.assertThat().extractingPath("$.components.readinessState.status").isEqualTo("UP");
                });
    }

    @Test
    void _03_ShouldIgnoreTheDatabase_WhenCheckingLiveness() {
        assertThat(mvc.get().uri("/actuator/health/liveness"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.status").isEqualTo("UP");
                    json.assertThat().doesNotHavePath("$.components.db");
                });
    }
}
