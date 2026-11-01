package fr.uge.project.ruge.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO returned by GET /api/recipes/{id} — combines recipe data
 * with the authenticated user's interaction state.
 */
public record RecipeDetailWithInteractionsDto(

    Long id,
    String name,
    String description,
    String imageUrl,
    String creatorName,
    Long creatorId,
    List<InstructionDto> steps,
    List<IngredientDto> ingredients,
    int totalDuration,
    Long recipeSourceId,
    boolean active,

    long likeCount,
    long superLikeCount,
    BigDecimal superLikePrice,

    boolean isOwnRecipe,
    boolean isLikedRecipe,
    boolean isFollowingRecipe,
    boolean isSuperLikedRecipe,

    List<RecipeReviewDto> reviews
) {}