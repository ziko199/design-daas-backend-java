package de.frauas.design.backend.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the default administrator initialization.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.admin-initializer")
public class AdminInitializerProperties {

    /**
     * Enables creation of the default administrator during application startup.
     *
     * <p>This should normally be disabled in production environments.</p>
     */
    private boolean enabled = false;

    /**
     * Email address of the administrator to create.
     */
    @NotBlank
    @Email
    private String email = "admin@design.local";

    /**
     * Initial password of the administrator.
     *
     * <p>This value should be supplied through a secure environment variable
     * or secret management system in environments where initialization is enabled.</p>
     */
    @NotBlank
    private String password;
}
