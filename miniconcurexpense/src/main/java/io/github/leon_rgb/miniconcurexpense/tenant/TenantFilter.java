package io.github.leon_rgb.miniconcurexpense.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * Servlet filter that extracts the tenant identifier from the "X-Tenant" header
 * and sets it in the TenantContext for the duration of the request.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String tenant = request.getHeader("X-Tenant");
        
        // Default to "public" if no tenant header is provided
        if (tenant == null || tenant.isBlank()) {
            tenant = "public";
        }
        
        System.out.println("Processing request for tenant: " + tenant);
        
        try {
            TenantContext.setCurrentTenant(tenant);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}