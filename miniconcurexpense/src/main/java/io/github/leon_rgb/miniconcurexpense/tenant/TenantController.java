package io.github.leon_rgb.miniconcurexpense.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for creating new tenants. Each tenant corresponds to a separate PostgreSQL schema.
 */
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/{tenantId}")
    public String createTenant(@PathVariable String tenantId) {
        tenantService.createTenant(tenantId);
        return "Tenant created: " + tenantId;
    }
}
