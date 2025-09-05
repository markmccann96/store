package com.example.store.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This class is for configuring for exposing the header values we set in the queries. These include:
 * - X-Total-Count - the total count of orders on some fetches
 * - Link - the links to subsequent pages when viewed per page
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Total-Count","Link","ETag","Last-Modified")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}