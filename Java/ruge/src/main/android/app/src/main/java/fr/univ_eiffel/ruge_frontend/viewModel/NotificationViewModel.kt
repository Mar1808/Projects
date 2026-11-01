package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.NotificationDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.services.NotificationApiService
import fr.univ_eiffel.ruge_frontend.services.interfaces.NotificationServiceInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationViewModel(private val displayService: NotificationServiceInterface) : ViewModel() {

    // -1L signifie que l'utilisateur vient juste de se connecter (aucune vérification faite)
    private var lastId: Long = -1L

    var notifications by mutableStateOf<List<NotificationDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun startChecking() {
        viewModelScope.launch {
            while (true) {
                if (DataService.isLoggedIn) {
                    loadLatest()
                } else {
                    // L'utilisateur s'est déconnecté. On remet le verrou à -1L.
                    // Ainsi, à sa prochaine connexion, il recevra la dernière notification.
                    lastId = -1L
                }
                delay(10000) // Vérification toutes les 10 secondes
            }
        }
    }

    private suspend fun loadLatest() {
        isLoading = true
        val response = NotificationApiService.getLatestNotifications(DataService.authToken)

        if (response != null && response.isSuccessful) {
            val list = response.body() ?: emptyList()
            notifications = list

            val latest = list.firstOrNull()

            if (latest != null) {
                if (lastId == -1L) {
                    lastId = latest.id
                }
                else if (latest.id > lastId) {
                    displayService.showSimpleNotification(latest.title, latest.message)
                    lastId = latest.id
                }
            }
        }
        isLoading = false
    }
}