package io.github.leon_rgb.miniconcurexpense.tenant;

/**
 * Utility class to hold the current tenant identifier in a ThreadLocal variable. 
 * This means that the tenant context is specific to the current thread and won't interfere with other threads.
 * This is crucial in a multi-tenant application where multiple requests (threads) may be handled concurrently.
 */
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
