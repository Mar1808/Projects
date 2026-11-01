package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.WalletInfoDto
import fr.univ_eiffel.ruge_frontend.services.WalletService
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {

    var walletInfo by mutableStateOf<WalletInfoDto?>(null)
    var isLoading by mutableStateOf(false)
    var messageInfo by mutableStateOf<String?>(null) // Pour afficher des succès ou erreurs
    var urlToOpen by mutableStateOf<String?>(null)   // Pour ouvrir le navigateur (Stripe)

    init {
        loadWallet()
    }

    fun loadWallet() {
        viewModelScope.launch {
            isLoading = true

            val response = WalletService.getWalletInfo()

            if (response != null && response.isSuccessful) {
                walletInfo = response.body()
            } else {
                messageInfo = "Erreur lors du chargement de votre wallet."
            }
            isLoading = false
        }
    }

    fun rechargeWallet(amount: String) {
        if (amount.isBlank()) {
            messageInfo = "Veuillez entrer un montant valide."
            return
        }

        viewModelScope.launch {
            isLoading = true
            val response = WalletService.createCheckout(amount)

            if (response != null && response.isSuccessful) {
                // On dit à la vue d'ouvrir l'URL Stripe
                urlToOpen = response.body()?.checkoutUrl
            } else {
                messageInfo = "Erreur lors de la création du paiement."
            }
            isLoading = false
        }
    }

    fun withdrawFunds(amount: String) {
        if (amount.isBlank()) {
            messageInfo = "Veuillez entrer un montant valide."
            return
        }

        viewModelScope.launch {
            isLoading = true
            val response = WalletService.withdrawToBank(amount)

            if (response != null && response.isSuccessful) {
                messageInfo = response.body()?.message ?: "Demande de retrait effectuée"
                loadWallet()
            } else {
                messageInfo = "Erreur lors de la demande de retrait."
            }
            isLoading = false
        }
    }

    fun clearMessage() {
        messageInfo = null
    }

    fun urlHasBeenOpened() {
        urlToOpen = null
    }
}