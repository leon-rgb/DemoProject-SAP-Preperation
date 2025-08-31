package io.github.leon_rgb.miniconcurexpense.tenant;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Service to manage tenant schemas and run migrations.
 */
@Service
@RequiredArgsConstructor
public class TenantService {
    private final DataSource dataSource;

    public void createTenant(String tenant) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            // Check if schema already exists
            ResultSet rs = st.executeQuery(
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + tenant + "'"
            );
            
            if (!rs.next()) {
                // Schema doesn't exist, create it
                st.execute("CREATE SCHEMA \"" + tenant + "\"");
                System.out.println("Created schema: " + tenant);
                
                // Run Flyway migration against this tenant schema
                try {
                    Flyway flyway = Flyway.configure()
                            .dataSource(dataSource)
                            .schemas(tenant)
                            .locations("classpath:db/migration")
                            .baselineOnMigrate(true)
                            .load();
                    
                    flyway.migrate();
                    System.out.println("Successfully migrated schema: " + tenant);
                    
                    // Verify table creation
                    verifyTableExists(tenant);
                    
                } catch (Exception e) {
                    System.err.println("Flyway migration failed for tenant " + tenant + ": " + e.getMessage());
                    // Fallback: create table manually
                    createTableManually(tenant);
                }
            } else {
                System.out.println("Schema already exists: " + tenant);
                // Check if table exists in existing schema
                verifyTableExists(tenant);
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating tenant " + tenant + ": " + e.getMessage());
            throw new RuntimeException("Failed to create tenant: " + tenant, e);
        }
    }
    
    private void verifyTableExists(String tenant) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            // Set search path to the tenant schema
            st.execute("SET search_path TO \"" + tenant + "\"");
            
            // Check if expense table exists
            ResultSet rs = st.executeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = '" + tenant + "' AND table_name = 'expense'"
            );
            
            if (rs.next()) {
                System.out.println("Table 'expense' exists in schema: " + tenant);
            } else {
                System.out.println("Table 'expense' missing in schema: " + tenant + ", creating manually");
                createTableManually(tenant);
            }
            
        } catch (SQLException e) {
            System.err.println("Error verifying table for tenant " + tenant + ": " + e.getMessage());
        }
    }
    
    private void createTableManually(String tenant) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            // Set search path to the tenant schema
            st.execute("SET search_path TO \"" + tenant + "\"");
            
            // Create the expense table manually
            st.execute(
                "CREATE TABLE IF NOT EXISTS expense (" +
                "    id SERIAL PRIMARY KEY," +
                "    description TEXT NOT NULL," +
                "    amount NUMERIC(10,2) NOT NULL" +
                ")"
            );
            
            System.out.println("Manually created 'expense' table in schema: " + tenant);
            
        } catch (SQLException e) {
            System.err.println("Error manually creating table for tenant " + tenant + ": " + e.getMessage());
        }
    }
}