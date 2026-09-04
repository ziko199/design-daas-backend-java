package de.frauas.design.backend.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the application's OAuth2/JWT infrastructure.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    /**
     * Issuer URI written into issued JWTs and validated on incoming tokens.
     */
    @NotBlank
    private String issuer = "http://localhost:8080";

    /**
     * Directory in which the application's RSA JWK is persisted.
     */
    @NotBlank
    private String keysDir = "./keys";
}
