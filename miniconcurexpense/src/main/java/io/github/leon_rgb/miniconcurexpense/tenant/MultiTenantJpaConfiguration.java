package io.github.leon_rgb.miniconcurexpense.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA configuration for multi-tenant setup using schema-based multi-tenancy.
 */
@Configuration
public class MultiTenantJpaConfiguration {

    private final DataSource dataSource;
    private final SchemaPerTenantConnectionProvider connectionProvider;
    private final HeaderTenantIdentifierResolver tenantIdentifierResolver;

    public MultiTenantJpaConfiguration(DataSource dataSource,
                                       SchemaPerTenantConnectionProvider connectionProvider,
                                       HeaderTenantIdentifierResolver tenantIdentifierResolver) {
        this.dataSource = dataSource;
        this.connectionProvider = connectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * Create the entity manager factory with multi-tenancy settings.
     * @param builder the entity manager factory builder
     * @return the configured entity manager factory bean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder) {
        Map<String, Object> props = new HashMap<>();
        
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        
        // Enable schema-per-tenant multi-tenancy
        props.put("hibernate.multiTenancy", "SCHEMA");

        return builder
                .dataSource(dataSource)
                .packages("io.github.leon_rgb.miniconcurexpense.model")
                .properties(props)
                .build();
    }
}