//package com.norvya.norvya.config;
//
//import org.springframework.beans.factory.annotation.Value;  // ✅ CORRECT
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//    @Value("${app.cors.allowed-origins}")
//    private String allowedOrigins;
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        // ✅ Lire depuis les properties
//        config.setAllowedOrigins(
//                List.of(allowedOrigins.split(","))
//        );
//
//        // ✅ Autoriser ton frontend Vercel + localhost dev
//        config.setAllowedOrigins(List.of(
//                "https://norvyastudy.vercel.app",
//                "https://norvyastudy-fcesb7cgl-pichoudevs-projects.vercel.app",
//                "http://localhost:3000",
//                "http://localhost:3001",
//                "*"
//        ));
//
//        // ✅ Méthodes HTTP autorisées
//        config.setAllowedMethods(List.of(
//                "GET", "POST", "PUT", "DELETE",
//                "PATCH", "OPTIONS" ,"*"
//        ));
//
//        // ✅ Headers autorisés
//        config.setAllowedHeaders(List.of(
//                "Authorization",
//                "Content-Type",
//                "Accept",
//                "Origin",
//                "X-Requested-With",
//                "Access-Control-Request-Method",
//                "Access-Control-Request-Headers",
//                "*"
//        ));
//
//        // ✅ Exposer le header Authorization dans la réponse
//        config.setExposedHeaders(List.of(
//                "Authorization",
//                "Access-Control-Allow-Origin",
//                "*"
//        ));
//
//        // ✅ Autoriser les cookies et credentials
//        config.setAllowCredentials(true);
//
//        // ✅ Cache preflight 1 heure
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source =
//                new UrlBasedCorsConfigurationSource();
//
//        // Appliquer sur toutes les routes
//        source.registerCorsConfiguration("/**", config);
//
//        return source;
//    }
//}


package com.norvya.norvya.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:https://norvyastudy.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ SOLUTION 1: Utiliser allowedOriginPatterns (permet * avec credentials)
        config.setAllowedOriginPatterns(List.of("*"));  // ← ÇA fonctionne avec allowCredentials(true) !

        // ✅ OU SOLUTION 2: Lister explicitement les origines
        // config.setAllowedOrigins(List.of(
        //     "https://norvyastudy.vercel.app",
        //     "https://norvyastudy-fcesb7cgl-pichoudevs-projects.vercel.app",
        //     "http://localhost:3000",
        //     "http://localhost:3001"
        // ));

        // ✅ Méthodes autorisées
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // ✅ Headers autorisés
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // ✅ Headers exposés
        config.setExposedHeaders(List.of("Authorization"));

        // ✅ Autoriser les credentials
        config.setAllowCredentials(true);

        // ✅ Cache preflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}