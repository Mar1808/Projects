package fr.univ_eiffel.ruge_frontend.dto

data class SimpleRecipeDto(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val totalDuration: Int,
    val creatorName : String
)