package fr.univ_eiffel.ruge_frontend.viewModel

import android.graphics.Bitmap // <-- AJOUT
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.RecipeCreateDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeDetailWithInteractionsDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeVariantDto
import fr.univ_eiffel.ruge_frontend.dto.SimpleRecipeDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.services.RecipeService
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {

    var createStatus by mutableStateOf<String?>(null)
    var createVariantStatus by mutableStateOf<String?>(null)

    var recipes by mutableStateOf<List<SimpleRecipeDto>>(emptyList())
        private set

    var recipeDetail by mutableStateOf<RecipeDetailWithInteractionsDto?>(null)
        private set

    val recipe get() = recipeDetail

    var isLiked      by mutableStateOf(false); private set
    var isFollowing  by mutableStateOf(false); private set
    var isSuperLiked by mutableStateOf(false); private set
    var isOwnRecipe  by mutableStateOf(false); private set
    var likeCount    by mutableStateOf(0L);    private set

    var errorMessage   by mutableStateOf<String?>(null); private set
    var successMessage by mutableStateOf<String?>(null); private set
    var isLoading      by mutableStateOf(false);         private set

    private fun requireToken(actionLabel: String): String? {
        if (!DataService.isLoggedIn || DataService.authToken == null) {
            errorMessage = "Connectez-vous pour $actionLabel."
            return null
        }
        return DataService.authToken
    }

    fun loadRecipes(page : Int = 0) {
        viewModelScope.launch {
            try {
                recipes += RecipeService.getRecipes(page)
            } catch (_: Exception) {
                errorMessage = "Impossible de charger les recettes."
            }
        }
    }

    fun searchRecipes(query: String) {
        viewModelScope.launch {
            try {
                recipes = RecipeService.searchRecipes(query, "standard")
            } catch (e: Exception) {
                errorMessage = "Erreur lors de la recherche."
            }
        }
    }

    fun searchRecipesAi(query: String) {
        viewModelScope.launch {
            try {
                recipes = RecipeService.searchRecipes(query, "ai")
            } catch (e: Exception) {
                errorMessage = "Erreur lors de la recherche."
            }
        }
    }


    fun loadRecipeById(id: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = DataService.authToken
                val detail = RecipeService.getRecipeWithInteractions(id, token)
                if (detail != null) {
                    recipeDetail = detail
                    isLiked      = detail.isLikedRecipe
                    isFollowing  = detail.isFollowingRecipe
                    isSuperLiked = detail.isSuperLikedRecipe
                    isOwnRecipe  = detail.isOwnRecipe
                    likeCount    = detail.likeCount
                }
            } catch (e: Exception) {
                errorMessage = "Recette introuvable."
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleLike(recipeId: Long) {
        val token = requireToken("liker") ?: return

        val wasLiked = isLiked
        isLiked   = !wasLiked
        likeCount = if (wasLiked) likeCount - 1 else likeCount + 1

        viewModelScope.launch {
            try {
                if (wasLiked) RecipeService.unlikeRecipe(recipeId, token)
                else          RecipeService.likeRecipe(recipeId, token)
            } catch (e: Exception) {
                isLiked   = wasLiked
                likeCount = if (wasLiked) likeCount + 1 else likeCount - 1
                errorMessage = e.message ?: "Erreur lors du like."
            }
        }
    }

    fun toggleFollow(recipeId: Long) {
        val token = requireToken("suivre cette recette") ?: return

        val wasFollowing = isFollowing
        isFollowing = !wasFollowing

        viewModelScope.launch {
            try {
                if (wasFollowing) RecipeService.unfollowRecipe(recipeId, token)
                else              RecipeService.followRecipe(recipeId, token)
            } catch (e: Exception) {
                isFollowing  = wasFollowing
                errorMessage = e.message ?: "Erreur lors du suivi."
            }
        }
    }

    fun superLike(recipeId: Long) {
        val token = requireToken("super liker") ?: return

        viewModelScope.launch {
            try {
                RecipeService.superLikeRecipe(recipeId, token)
                isSuperLiked   = true
                successMessage = "Super like envoyé !"
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erreur lors du super like."
            }
        }
    }

    fun postReview(recipeId: Long, comment: String, onSuccess: () -> Unit) {
        if (comment.isBlank()) {
            errorMessage = "Le commentaire ne peut pas être vide."
            return
        }
        val token = requireToken("laisser un avis") ?: return

        viewModelScope.launch {
            try {
                RecipeService.postReview(recipeId, comment, token)
                successMessage = "Avis publié !"
                onSuccess()
                loadRecipeById(recipeId)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erreur lors de la publication."
            }
        }
    }

    fun clearMessages() {
        errorMessage   = null
        successMessage = null
    }

    fun clearCreateStatus() {
        createStatus = null
    }

    fun clearCreateVariantStatus() {
        createVariantStatus = null
    }

    fun createRecipe(recipe: RecipeCreateDto, bitmap: Bitmap? = null) {
        val token = requireToken("créer une recette") ?: run {
            createStatus = "ERROR Connexion requise"
            return
        }
        viewModelScope.launch {
            createStatus = "LOADING"
            createStatus = RecipeService.createRecipe(token, recipe, bitmap)
        }
    }

    fun createVariant(recipeId: Long, variant: RecipeVariantDto) {
        val token = requireToken("créer une variante") ?: run {
            createVariantStatus = "ERROR Connexion requise pour créer une variante"
            return
        }
        viewModelScope.launch {
            createVariantStatus = "LOADING"

            createVariantStatus = RecipeService.createVariant(recipeId, token, variant)
        }
    }
}