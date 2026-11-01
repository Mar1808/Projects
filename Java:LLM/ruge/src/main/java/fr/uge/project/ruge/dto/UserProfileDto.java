package fr.uge.project.ruge.dto;

import java.util.List;

public record UserProfileDto(
    Long id,
    String name,
    String imageUrl,
    List<SimpleRecipeDto> recipes
) {}
