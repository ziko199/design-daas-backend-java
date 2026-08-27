package de.frauas.design.backend.config;

import de.frauas.design.backend.shared.security.Authorities;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configures the API's stateless, JWT-based resource-server security filter chain:
 * public endpoints, CORS, CSRF, session policy, and OAuth2 resource-server JWT validation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/user").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/user/validate_email")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/oauth2/user/token").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/version").permitAll();
                    if (swaggerEnabled) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                                .permitAll();
                    } else {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                                .hasAuthority(Authorities.SCOPE_ADMIN);
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }
}
