package fr.univ_eiffel.ruge_frontend.dto

data class BanRequestDto(
    val reason: String,
    val durationDays: Int
)

data class BanLogDto(
    val id: Long,
    val bannedUserId: Long,
    val bannedById: Long,
    val banDate: String,
    val reason: String,
    val duration: Int,
)

data class UserBanHistoryDto(
    val user: UserDisplayDto,
    val history: List<BanLogDto>
)