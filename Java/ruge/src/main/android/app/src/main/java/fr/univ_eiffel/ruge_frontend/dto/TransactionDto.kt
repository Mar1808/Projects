package fr.univ_eiffel.ruge_frontend.dto

data class TransactionDto(
    val id: Long,
    val date: String,
    val sourceUserId: Long,
    val targetUserId: Long,
    val amount: Double,
    val description: String,
    val transactionType: String,
    val referenceId: Long,
    val sourceRecipeId: Long,
    val targetRecipeId: Long
)