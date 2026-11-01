package fr.univ_eiffel.ruge_frontend.dto

data class PromotionDto(
    val id: Long,
    val type: String,
    val promoPrice: Double,
    val startDate: String,
    val endDate: String,
    val active: Boolean
)