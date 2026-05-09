package com.mario.stremio_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class StremioBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StremioBackendApplication.class, args);
    }

    // ¡NUEVO! Este bloque elimina cualquier bloqueo de CORS para Stremio
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "OPTIONS") // Permite la petición invisible de Stremio
                        .allowedHeaders("*");
            }
        };
    }
}