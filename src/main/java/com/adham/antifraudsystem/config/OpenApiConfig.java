package com.adham.antifraudsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI antiFraudOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Anti-Fraud System API")
                        .version("1.0.0")
                        .description("""
                                Rule-based transaction risk evaluation with user administration,
                                fraud blacklists, transaction history, and specialist feedback.
                                Use synthetic card numbers only.
                                """))
                .components(new Components().addSecuritySchemes(
                        BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic authentication with an application user")
                ));
    }
}
