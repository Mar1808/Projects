package fr.uge.project.ruge.dto;

import fr.uge.project.ruge.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transfer object for user creation and display.
 *
 * @param email login identifier
 * @param password plain text (registration) or hashed password
 * @param name full name or username
 * @param role access level
 */
public record UserDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    String imageUrl,

    Long profilePhotoId,

    Role role
) {
}
