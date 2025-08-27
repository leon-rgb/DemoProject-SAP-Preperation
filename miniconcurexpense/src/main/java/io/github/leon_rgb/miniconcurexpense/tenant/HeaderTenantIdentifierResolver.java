package io.github.leon_rgb.miniconcurexpense.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class HeaderTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
  public String resolveCurrentTenantIdentifier() {
    return Optional.ofNullable(TenantContext.getCurrentTenant()).orElse("public");
  }
  public boolean validateExistingCurrentSessions() { return true; }
}
