package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.PromotionDto
import fr.univ_eiffel.ruge_frontend.services.PromotionService
import kotlinx.coroutines.launch

class PromotionViewModel : ViewModel() {

    // L'état de la liste que Compose va observer
    var promotions by mutableStateOf<List<PromotionDto>>(emptyList())
        private set

    // L'état de chargement
    var isLoading by mutableStateOf(true)
        private set

    // Charge les promotions depuis l'API
    fun loadPromotions() {
        viewModelScope.launch {
            isLoading = true
            val response = PromotionService.getPromotions()
            if (response != null && response.isSuccessful) {
                promotions = response.body() ?: emptyList()
            } else {
                promotions = emptyList()
            }
            isLoading = false
        }
    }

    // Crée une promotion et rafraîchit la liste si succès
    fun createPromotion(type: String, price: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            val response = PromotionService.createPromotion(type, price, startDate, endDate)
            if (response != null && response.isSuccessful) {
                loadPromotions()
            } else {
                println("ERREUR BOUTON PROMO : Code HTTP = ${response?.code()}")
            }
        }
    }

    // Supprime une promotion et rafraîchit la liste si succès
    fun deletePromotion(id: Long) {
        viewModelScope.launch {
            val response = PromotionService.deletePromotion(id)
            if (response != null && response.isSuccessful) {
                loadPromotions() // Rafraîchissement automatique
            }
        }
    }
}