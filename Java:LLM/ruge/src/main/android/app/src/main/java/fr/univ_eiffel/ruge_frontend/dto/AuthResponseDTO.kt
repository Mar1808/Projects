package fr.univ_eiffel.ruge_frontend.dto

data class AuthResponseDTO(
    val token: String,
    val role: String,
    val name: String,
    val email: String,
    val imageUrl: String,
    val userId: Long
)