package io.github.leon_rgb.miniconcurexpense.tenant;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DebugController provides endpoints to inspect the current tenant context,
 * available schemas, tables in a schema, and the current search_path.
 * For Debugging purposes only.
 */
@RestController
@RequestMapping("/debug")
public class DebugController {
    
    private final DataSource dataSource;
    
    public DebugController(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @GetMapping("/current-tenant")
    public Map<String, String> getCurrentTenant() {
        Map<String, String> result = new HashMap<>();
        result.put("currentTenant", TenantContext.getCurrentTenant());
        return result;
    }
    
    @GetMapping("/schemas")
    public List<String> getSchemas() {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            ResultSet rs = st.executeQuery(
                "SELECT schema_name FROM information_schema.schemata " +
                "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'pg_temp_1', 'pg_toast_temp_1')"
            );
            
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
            
        } catch (Exception e) {
            schemas.add("Error: " + e.getMessage());
        }
        
        return schemas;
    }
    
    @GetMapping("/tables/{schema}")
    public List<String> getTablesInSchema(@PathVariable String schema) {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            ResultSet rs = st.executeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = '" + schema + "'"
            );
            
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
            
        } catch (Exception e) {
            tables.add("Error: " + e.getMessage());
        }
        
        return tables;
    }
    
    @GetMapping("/search-path")
    public Map<String, String> getSearchPath() {
        Map<String, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            
            ResultSet rs = st.executeQuery("SHOW search_path");
            if (rs.next()) {
                result.put("search_path", rs.getString(1));
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}