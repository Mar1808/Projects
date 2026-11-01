package fr.univ_eiffel.ruge_frontend.dto

data class RecipeDetailWithInteractionsDto(
    val id: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val creatorName: String,
    val creatorId: Long,
    val steps: List<InstructionDto>,
    val ingredients: List<IngredientDto>,
    val totalDuration: Int,
    val recipeSourceId: Long,
    val active: Boolean,
    // Counts
    val likeCount: Long,
    val superLikeCount: Long,
    val superLikePrice: Double,
    // Interaction state
    val isOwnRecipe: Boolean,
    val isLikedRecipe: Boolean,
    val isFollowingRecipe: Boolean,
    val isSuperLikedRecipe: Boolean,
    // Reviews
    val reviews: List<RecipeReviewDto>
)

data class RecipeReviewDto(
    val authorName: String,
    val authorId: Long,
    val comment: String,
    val dateReview: String
)