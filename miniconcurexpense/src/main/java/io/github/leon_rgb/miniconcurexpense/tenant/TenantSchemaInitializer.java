package io.github.leon_rgb.miniconcurexpense.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default tenant schemas on application startup.
 */
@Component
@RequiredArgsConstructor
public class TenantSchemaInitializer implements CommandLineRunner {
    private final TenantService tenantService;

    @Override
    public void run(String... args) {
        // Create default tenants on startup
        String[] defaultTenants = {"sap", "ibm", "public"};
        
        for (String tenant : defaultTenants) {
            try {
                tenantService.createTenant(tenant);
                System.out.println("✔ Tenant '" + tenant + "' is ready.");
            } catch (Exception e) {
                System.err.println("✖ Failed to initialize tenant '" + tenant + "': " + e.getMessage());
            }
        }
    }
}