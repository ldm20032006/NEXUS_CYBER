package demo.server.foundation;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTests {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexus")
            .withUsername("nexus")
            .withPassword("nexus");

    @Test
    void flywayMigratesEmptyPostgresDatabase() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     SELECT column_name
                     FROM information_schema.columns
                     WHERE table_name = 'database_baseline'
                     ORDER BY ordinal_position
                     """)) {

            var columns = new java.util.ArrayList<String>();
            while (resultSet.next()) {
                columns.add(resultSet.getString("column_name"));
            }

            assertThat(columns).contains(
                    "id",
                    "created_at",
                    "updated_at",
                    "created_by",
                    "updated_by",
                    "deleted",
                    "deleted_at",
                    "version"
            );
        }
    }
}
