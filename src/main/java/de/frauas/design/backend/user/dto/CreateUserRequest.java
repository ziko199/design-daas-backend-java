package de.frauas.design.backend.user.dto;

import de.frauas.design.backend.shared.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * Request payload for registering a regular user account.
 */
@Data
public class CreateUserRequest {

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

    /** Optional — assign the user to these UserGroup IDs on creation. */
    private List<Integer> groups;
}
