package fr.uge.project.ruge.dto;

import java.time.LocalDateTime;

public record UserLikeReactionDto(
    Long recipeId,
    String recipeName,
    LocalDateTime likedAt
) {}
