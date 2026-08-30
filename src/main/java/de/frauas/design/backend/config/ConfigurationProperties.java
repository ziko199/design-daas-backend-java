package de.frauas.design.backend.config;

import de.frauas.design.backend.config.properties.CorsProperties;
import de.frauas.design.backend.config.properties.OAuth2Properties;
import de.frauas.design.backend.config.properties.RateLimitingProperties;
import de.frauas.design.backend.config.properties.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    RateLimitingProperties.class,
    SecurityProperties.class,
    CorsProperties.class,
    OAuth2Properties.class
})
public class ConfigurationProperties {}
