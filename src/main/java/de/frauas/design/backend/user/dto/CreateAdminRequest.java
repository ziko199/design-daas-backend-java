package de.frauas.design.backend.user.dto;

import de.frauas.design.backend.user.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Request payload for creating an administrator account. */
@Data
public class CreateAdminRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(
            regexp = PasswordPolicy.PASSWORD_REGEX,
            message = "password must be at least 8 characters and contain uppercase, lowercase, and a digit")
    private String password;
}
