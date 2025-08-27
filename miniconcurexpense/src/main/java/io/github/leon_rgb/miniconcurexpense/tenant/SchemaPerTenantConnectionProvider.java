package io.github.leon_rgb.miniconcurexpense.tenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<Connection> {
  private final DataSource dataSource;

  public Connection getAnyConnection() throws SQLException { return dataSource.getConnection(); }
  public Connection getConnection(String tenant) throws SQLException {
    Connection c = dataSource.getConnection();
    try (Statement st = c.createStatement()) {
      st.execute("set search_path to \"" + tenant + "\", public");
    }
    return c;
  }
  public void releaseAnyConnection(Connection c) throws SQLException { c.close(); }
  public void releaseConnection(String tenant, Connection c) throws SQLException {
    try (Statement st = c.createStatement()) { st.execute("set search_path to public"); }
    finally { c.close(); }
  }
  public boolean isUnwrappableAs(Class<?> a){ return false; }
  public <T> T unwrap(Class<T> a){ return null; }
  public boolean supportsAggressiveRelease(){ return false; }

  @Override
  public Connection getConnection(Connection arg0) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getConnection'");
  }
  @Override
  public void releaseConnection(Connection arg0, Connection arg1) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'releaseConnection'");
  }
}
