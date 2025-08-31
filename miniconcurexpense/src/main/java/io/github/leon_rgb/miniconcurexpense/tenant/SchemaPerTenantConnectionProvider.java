package io.github.leon_rgb.miniconcurexpense.tenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Connection provider that sets the PostgreSQL schema based on the tenant identifier.
 */
@Component
@RequiredArgsConstructor
public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    
    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        try (Statement statement = connection.createStatement()) {
            // Set the schema for this tenant - note the correct syntax
            System.out.println("Setting search_path to: " + tenantIdentifier);
            statement.execute("SET search_path TO \"" + tenantIdentifier + "\"");
            
            // Verify the search path was set
            ResultSet rs = statement.executeQuery("SHOW search_path");
            if (rs.next()) {
                System.out.println("Current search_path: " + rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error setting search_path for tenant " + tenantIdentifier + ": " + e.getMessage());
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Reset to public schema
            statement.execute("SET search_path TO public");
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}