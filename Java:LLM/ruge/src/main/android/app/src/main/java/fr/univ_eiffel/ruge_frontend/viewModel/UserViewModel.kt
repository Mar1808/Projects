package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.SimpleRecipeDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.services.UserService
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    var myRecipes by mutableStateOf<List<SimpleRecipeDto>>(emptyList())

    var reviewCount by mutableIntStateOf(0)
        private set

    var likeCount by mutableIntStateOf(0)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadMyRecipesAndStats() {
        viewModelScope.launch {
            try {
                // Chargement des recettes
                val token = DataService.authToken ?: ""
                myRecipes = UserService.loadMyRecipes(token)

                // Chargement des statistiques (Likes et Avis reçus)
                val userId = DataService.userId
                if (userId != null) {
                    val stats = UserService.getUserStats(userId)
                    if (stats != null) {
                        reviewCount = stats.reviewCount
                        likeCount = stats.likeCount
                        println("Stats chargées : Likes=${likeCount}, Avis=${reviewCount}")
                    }
                }
            } catch (e: Exception) {
                println("Erreur chargement stats : ${e.localizedMessage}")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            errorMessage = null
            val returnValue = UserService.loginUser(email, password)
            if (returnValue != null) {
                errorMessage = returnValue
            }
        }
    }

    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            val success = UserService.createUser(username, email, password)
            if (success == "OK") {
                println("User registered successfully")
            } else {
                println("Error registering user: $success")
            }
        }
    }
}