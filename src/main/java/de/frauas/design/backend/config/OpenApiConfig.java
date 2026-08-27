package de.frauas.design.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI/Swagger UI document: general API metadata plus the bearer-JWT
 * security scheme used by every protected endpoint.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI document, registering {@code bearerAuth} as a JWT bearer
     * security scheme so Swagger UI can send {@code Authorization: Bearer <token>}.
     *
     * @return the configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Design DaaS Backend API")
                        .version("1.0.0")
                        .description("Spring Boot backend replacing the PHP backend"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
