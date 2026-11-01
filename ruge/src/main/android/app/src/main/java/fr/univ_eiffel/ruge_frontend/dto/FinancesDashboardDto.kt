package fr.univ_eiffel.ruge_frontend.dto

data class FinancesDashboardDto(
    val totalRedistribution: Double,
    val totalBenefits: Double,
    val aiSearchPrice: Double,
    val superLikePrice: Double,
    val transactions: List<TransactionDto>
)
