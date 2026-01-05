package com.egov.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void corsConfiguration_ShouldAllowSpecificOriginAndHeaders() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        
        CorsConfiguration cors = source.getCorsConfiguration(exchange);

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(cors.getAllowedHeaders().contains("X-USER-ID"));
        assertTrue(cors.getAllowedHeaders().contains("X-USER-ROLE"));
        assertTrue(cors.getAllowCredentials());
    }
}