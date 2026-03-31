package com.food.payment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("API for managing payments in the Food Ordering System")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8000").description("API Gateway (Port 8000)"),
                        new Server().url("http://localhost:8004").description("Payment Service Direct (Port 8004)")
                ));
    }
}
