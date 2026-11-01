package fr.univ_eiffel.ruge_frontend.dto

data class RecipeVariantDto(
    val originalRecipeId: Long,
    val name: String,
    val description: String,
    val steps: List<RecipeVariantStepDto>,
    val ingredients: List<RecipeVariantIngredientDto>
)

data class RecipeVariantStepDto(
    val step: String,
    val duration: Int,
    val deleted: Boolean,
    val original: Boolean
)

data class RecipeVariantIngredientDto(
    val name: String,
    val quantity: String,
    val deleted: Boolean,
    val original: Boolean
)
