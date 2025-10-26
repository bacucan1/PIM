package com.example.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para pruebas
 * Deshabilita la autenticación para permitir que los tests funcionen correctamente
 * 
 * @TestConfiguration indica que esta configuración solo se usa en tests
 * @Primary hace que esta configuración tenga prioridad sobre la de producción
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Configura Spring Security para tests
     * Permite todas las peticiones sin autenticación
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para tests
            .csrf(csrf -> csrf.disable())
            
            // Permitir todas las peticiones
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}