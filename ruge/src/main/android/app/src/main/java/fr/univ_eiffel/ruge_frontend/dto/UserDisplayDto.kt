package fr.univ_eiffel.ruge_frontend.dto

data class UserDisplayDto(
    val id: Long,
    val email: String,
    val name: String,
    val imageUrl: String,
    val role: String,
    val status: String
)