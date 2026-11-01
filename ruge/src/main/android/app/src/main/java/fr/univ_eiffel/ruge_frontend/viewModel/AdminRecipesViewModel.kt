package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.RecipeDetailDto
import fr.univ_eiffel.ruge_frontend.services.AdminService
import kotlinx.coroutines.launch

class AdminRecipesViewModel : ViewModel() {
    private var allRecipes = emptyList<RecipeDetailDto>()
    
    var recipes by mutableStateOf<List<RecipeDetailDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            isLoading = true
            allRecipes = AdminService.getRecipes()
            applyFilter()
            isLoading = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        if (searchQuery.isBlank()) {
            recipes = allRecipes
        } else {
            recipes = allRecipes.filter { recipe ->
                recipe.name.contains(searchQuery, ignoreCase = true) ||
                recipe.creatorName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            val isSuccess = AdminService.deleteRecipe(recipeId)
            if (isSuccess) {
                allRecipes = allRecipes.filter { it.id != recipeId }
                applyFilter() // Met à jour la liste affichée
            } else {
                println("Erreur lors de la suppression de la recette.")
            }
        }
    }
}