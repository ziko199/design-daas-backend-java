package de.frauas.design.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ValidateEmailRequest {
    private String email;

    @JsonProperty("registration_code")
    private String registrationCode;
}
