package com.food.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8000").description("API Gateway (Port 8000)"),
                        new Server().url("http://localhost:8001").description("User Service (Port 8001)"),
                        new Server().url("http://localhost:8002").description("Menu Service (Port 8002)")
                ));
    }
}
