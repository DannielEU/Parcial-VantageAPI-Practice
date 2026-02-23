package com.api.parcial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS (Cross-Origin Resource Sharing).
 * Permite que el frontend desde otros dominios acceda a esta API.
 * 
 * CORS enables requests from:
 * - http://localhost:3000 (frontend local)
 * - https://yourdomain.com (frontend en producción)
 * - Cualquier origen si se configura allowedOrigins("*")
 */
@Configuration
public class CorsConfig {

    /**
     * Configura CORS para toda la aplicación.
     * Permite solicitudes desde cualquier origen (considerar restringir en producción).
     * @return Configurador de CORS
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")                    // Aplica a todos los endpoints
                        .allowedOrigins("*")                // Permite cualquier origen (INSEGURO en prod)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Métodos permitidos
                        .allowedHeaders("*")                 // Headers permitidos
                        .allowCredentials(false)             // No permite cookies
                        .maxAge(3600);                       // Cache CORS por 1 hora
            }
        };
    }
}