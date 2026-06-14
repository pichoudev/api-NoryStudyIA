package com.norvya.norvya.config;

import com.google.api.client.util.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // ✅ Lire depuis les properties
        config.setAllowedOrigins(
                List.of(allowedOrigins.split(","))
        );

        // ✅ Autoriser ton frontend Vercel + localhost dev
        config.setAllowedOrigins(List.of(
                "https://norvyastudy.vercel.app",
                "http://localhost:3000",
                "http://localhost:3001"
        ));

        // ✅ Méthodes HTTP autorisées
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE",
                "PATCH", "OPTIONS"
        ));

        // ✅ Headers autorisés
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // ✅ Exposer le header Authorization dans la réponse
        config.setExposedHeaders(List.of(
                "Authorization",
                "Access-Control-Allow-Origin"
        ));

        // ✅ Autoriser les cookies et credentials
        config.setAllowCredentials(true);

        // ✅ Cache preflight 1 heure
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // Appliquer sur toutes les routes
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}