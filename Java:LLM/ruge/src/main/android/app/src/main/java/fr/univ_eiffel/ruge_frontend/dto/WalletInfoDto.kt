package fr.univ_eiffel.ruge_frontend.dto

data class WalletInfoDto(
    val walletBalance: Double,
    val paymentMinAmount: Double,
    val paymentMaxAmount: Double,
    val connectStatus: ConnectAccountStatusDto?,
    val connectErrorMessage: String?,
    val redistributionDetails: List<RedistributionDetailsDto>,
    val superLikeSent: Int
)

data class ConnectAccountStatusDto(
    val accountLinked: Boolean,
    val bankAccountReady: Boolean,
    val connectAccountId: String?
)

data class RedistributionDetailsDto(
    val date: String,
    val recetteSource: String,
    val recetteBeneficiaire: String,
    val amount: Double
)

data class CheckoutResponseDto(
    val checkoutUrl: String
)

data class OnboardingResponseDto(
    val onboardingUrl: String
)

data class MessageResponseDto(
    val message: String?,
    val error: String?
)