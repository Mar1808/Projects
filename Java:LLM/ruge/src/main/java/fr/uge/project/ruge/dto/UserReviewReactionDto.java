package fr.uge.project.ruge.dto;

import java.time.LocalDateTime;

public record UserReviewReactionDto(
    Long recipeId,
    String recipeName,
    String comment,
    LocalDateTime dateReview
) {}
