package fr.uge.project.ruge.dto;

import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;

public record UserDisplayDto(
    Long id,
    String email,
    String name,
    String imageUrl,
    Role role,
    Status status
) {}