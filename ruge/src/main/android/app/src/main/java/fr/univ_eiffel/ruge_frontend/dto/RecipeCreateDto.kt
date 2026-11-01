package fr.univ_eiffel.ruge_frontend.dto

data class RecipeCreateDto(
    val name: String,
    val description: String,
    val imageUrl: String,
    val totalDuration: Int,
    val ingredients: List<RecipeCreateIngredientDto>,
    val steps: List<RecipeCreateInstructionDto>
)
