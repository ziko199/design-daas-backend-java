package de.frauas.design.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Request payload for email-verification of a newly registered user account. */
@Data
public class ValidateEmailRequest {
    private String email;

    @JsonProperty("registration_code")
    private String registrationCode;
}
