package com.edgareldy.springmodulithtutorial.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Checks what the V1 migration leaves in a real PostgreSQL database.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Same annotations as HealthEndpointsTest on purpose: Spring's test context cache then reuses one
// application context, and therefore one PostgreSQL container, for both classes.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FlywaySchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void _01_ShouldHaveAppliedV1Successfully_WhenTheApplicationStarts() {
        Boolean success = jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);

        assertThat(success).isTrue();
    }

    @Test
    void _02_ShouldCreateTheTablesOfEveryModuleAndTheEventRegistry_WhenV1IsApplied() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains(
                "users", "roles", "role_user", "activation_tokens", "blacklisted_tokens", "password_reset_tokens",
                "categories", "products", "customers", "orders", "event_publication");
    }

    @Test
    void _03_ShouldSeedTheAdminAndUserRoles_WhenV1IsApplied() {
        List<String> roles = jdbc.queryForList("SELECT role_name FROM roles", String.class);

        assertThat(roles).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void _04_ShouldDeclareNoForeignKeyAcrossModules_WhenV1IsApplied() {
        // orders references customers and products by id only, and customers never references users:
        // module boundaries are not crossed by database constraints either.
        Integer crossModuleForeignKeys = jdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE constraint_type = 'FOREIGN KEY' AND table_name IN ('orders', 'customers')
                """, Integer.class);

        assertThat(crossModuleForeignKeys).isZero();
    }
}
