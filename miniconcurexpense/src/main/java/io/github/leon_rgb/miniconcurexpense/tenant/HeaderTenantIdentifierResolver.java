package io.github.leon_rgb.miniconcurexpense.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Resolves the current tenant identifier from the TenantContext.
 * The tenant is determined by a header in a HTTP request.
 */
@Component
public class HeaderTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
  public String resolveCurrentTenantIdentifier() {
    return Optional.ofNullable(TenantContext.getCurrentTenant()).orElse("public");
  }
  public boolean validateExistingCurrentSessions() { return true; }
}
