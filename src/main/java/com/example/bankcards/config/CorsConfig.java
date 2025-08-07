package com.example.bankcards.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
/**
 * Конфигурация CORS для API.
 */
public class CorsConfig {

    /** Включение/выключение CORS. */
    @Value("${app.security.cors.enabled:true}")
    private boolean corsEnabled;

    /** Разрешенные источники (origins). */
    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    /** Разрешенные HTTP-методы. */
    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String[] allowedMethods;

    /** Разрешенные заголовки. */
    @Value("${app.security.cors.allowed-headers:*}")
    private String[] allowedHeaders;

    /** Заголовки, которые будут выставлены клиенту. */
    @Value("${app.security.cors.exposed-headers:Authorization,Content-Type}")
    private String[] exposedHeaders;

    /** Разрешить ли передачу учетных данных (cookies, auth headers). */
    @Value("${app.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    /** Максимальный возраст preflight-запроса (в секундах). */
    @Value("${app.security.cors.max-age:3600}")
    private long maxAge;

    @Bean
    /**
     * Создает источник конфигурации CORS и регистрирует правила для /api/**.
     * @return источник конфигурации CORS
     */
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!corsEnabled) {
            return source; // empty configuration -> CORS effectively disabled
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList(allowedMethods));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders));
        config.setExposedHeaders(Arrays.asList(exposedHeaders));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        // Apply for API routes
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
