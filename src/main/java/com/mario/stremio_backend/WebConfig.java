package com.mario.stremio_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Le decimos a Spring que cualquier petición a /local-videos/
        // la busque en tu carpeta física de Descargas
        registry.addResourceHandler("/local-videos/**")
                .addResourceLocations("file:C:/Users/PC/Downloads/Torrent/");
    }
}