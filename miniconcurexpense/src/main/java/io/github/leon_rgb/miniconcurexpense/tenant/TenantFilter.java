package io.github.leon_rgb.miniconcurexpense.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
  @Override protected void doFilterInternal(@NonNull HttpServletRequest req, 
                                            @NonNull HttpServletResponse res, 
                                            @NonNull FilterChain chain)
      throws ServletException, IOException {
    String tenant = req.getHeader("X-Tenant");
    if (tenant == null || tenant.isBlank()) tenant = "public";
    try { TenantContext.setCurrentTenant(tenant); chain.doFilter(req, res); }
    finally { TenantContext.clear(); }
  }
}