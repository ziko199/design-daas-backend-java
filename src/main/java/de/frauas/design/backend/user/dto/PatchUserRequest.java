package de.frauas.design.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Partial-update request for an existing user.
 * All fields are optional — a {@code null} value means "do not change".
 *
 * <p>Validation constraints are applied so that if a field IS provided
 * it must still satisfy the same business rules as on creation.</p>
 */
@Data
public class PatchUserRequest {

    @Size(min = 1, max = 255, message = "name must be between 1 and 255 characters")
    private String name;

    @Email(message = "email must be a valid e-mail address")
    private String email;

    /** Same regex as CreateUserRequest / UserService.PASSWORD_REGEX. */
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
            message = "password must be at least 8 characters and contain uppercase, lowercase, and a digit")
    private String password;

    private List<Integer> groups;
}
