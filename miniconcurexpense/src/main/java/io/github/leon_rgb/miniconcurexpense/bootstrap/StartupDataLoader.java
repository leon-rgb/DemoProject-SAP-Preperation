package io.github.leon_rgb.miniconcurexpense.bootstrap;

import io.github.leon_rgb.miniconcurexpense.model.Expense;
import io.github.leon_rgb.miniconcurexpense.repository.ExpenseRepository;
import io.github.leon_rgb.miniconcurexpense.tenant.TenantContext;

import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * On application startup, ensure each non-system schema has at least 30 expenses.
 * Uses TenantContext to switch the tenant/schema before using the JPA repository,
 * so this is compatible with the request-time filter/tenant resolver that the app uses.
 */
@Component
public class StartupDataLoader implements ApplicationListener<ApplicationReadyEvent> {

    private final DataSource dataSource;
    private final ExpenseRepository expenseRepository;
    private final Random random = new Random();

    public StartupDataLoader(DataSource dataSource, ExpenseRepository expenseRepository) {
        this.dataSource = dataSource;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        List<String> schemas = fetchSchemas();
        for (String schema : schemas) {
            // skip system schemas just in case (same list as DebugController)
            if (isSystemSchema(schema)) continue;
            try {
                TenantContext.setCurrentTenant(schema);
                long count = expenseRepository.count();
                if (count < 30) {
                    int toCreate = 30 - (int) count;
                    for (int i = 0; i < toCreate; i++) {
                        Expense e = new Expense();
                        e.setDescription(randomDescription());
                        e.setAmount(randomAmount());
                        expenseRepository.save(e);
                    }
                    System.out.println("Inserted " + toCreate + " expenses for schema: " + schema);
                } else {
                    System.out.println("Schema " + schema + " already has " + count + " expenses.");
                }
            } catch (Exception ex) {
                System.err.println("Failed to seed expenses for schema " + schema + ": " + ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private List<String> fetchSchemas() {
        List<String> schemas = new ArrayList<>();
        String sql = "SELECT schema_name FROM information_schema.schemata " +
                     "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'pg_temp_1', 'pg_toast_temp_1')";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
        } catch (Exception ex) {
            System.err.println("Error fetching schemas: " + ex.getMessage());
        }
        return schemas;
    }

    private boolean isSystemSchema(String s) {
        if (s == null) return true;
        String lower = s.toLowerCase();
        return lower.startsWith("pg_") || lower.equals("information_schema") || lower.equals("public") == false && lower.equals("public"); // keep public, but skip obvious pg_ ones
    }

    // Generate a random description using a few parts for variety
    private String randomDescription() {
        String[] verbs = {"Taxi", "Lunch", "Dinner", "Hotel", "Flight", "Train", "Parking", "Coffee", "Supplies", "Taxi ride", "Uber", "Meal", "Conference fee", "Subscription", "Office chair"};
        String[] extras = {"for client", "team", "meeting", "travel", "reimbursement", "project A", "project B", "misc", "snack", "airport", "workshop", "training"};
        String v = verbs[random.nextInt(verbs.length)];
        String e = extras[random.nextInt(extras.length)];
        int suffix = 1 + random.nextInt(300);
        return v + " - " + e + " #" + suffix;
    }

    // random amount between 5.00 and 1000.00 with two decimals
    private Double randomAmount() {
        double min = 5.0;
        double max = 1000.0;
        double val = min + random.nextDouble() * (max - min);
        // round to two decimals
        double rounded = Math.round(val * 100.0) / 100.0;
        return rounded;
    }
}
