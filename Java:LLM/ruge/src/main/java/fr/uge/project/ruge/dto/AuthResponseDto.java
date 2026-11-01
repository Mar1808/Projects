package fr.uge.project.ruge.dto;

import fr.uge.project.ruge.enums.Role;

public record AuthResponseDto(String token, Role role, String name, String email, String imageUrl, Long userId) {
}
