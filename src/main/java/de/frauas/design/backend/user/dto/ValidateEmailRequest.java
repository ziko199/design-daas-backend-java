package de.frauas.design.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for email-verification of a newly registered user account.
 */
@Data
public class ValidateEmailRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @JsonProperty("registration_code")
    private String registrationCode;
}
