package fr.univ_eiffel.ruge_frontend.dto

data class RecipeDetailDto(
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
    val active: Boolean
)