package io.github.leon_rgb.miniconcurexpense;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/***
 * Configuration is only needed for local development with frontend running on a different port.
 * In production, the frontend would be served by the backend, so CORS is not an issue
 */
@Configuration
@Profile("dev")
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*"); // allow all hosts
            }
        };
    }
}
