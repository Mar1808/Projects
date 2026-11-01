package fr.univ_eiffel.ruge_frontend.services

import android.util.Log
import fr.univ_eiffel.ruge_frontend.dto.BanRequestDto
import fr.univ_eiffel.ruge_frontend.dto.FinancesDashboardDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeDetailDto
import fr.univ_eiffel.ruge_frontend.dto.UpdatePricesDto
import fr.univ_eiffel.ruge_frontend.dto.UserBanHistoryDto
import fr.univ_eiffel.ruge_frontend.dto.UserDisplayDto
import fr.univ_eiffel.ruge_frontend.services.interfaces.AdminServiceInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object AdminService {
    private const val BASE_URL = "http://10.0.2.2:8080/api/admin/"
    private val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BASE_URL).build()
    private val adminService =  retrofit.create<AdminServiceInterface>()


    suspend fun getUsers(): List<UserDisplayDto> {
        return try {
            adminService.getUsers(DataService.authToken)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getBanHistory(userId: Long): UserBanHistoryDto? {
        return try {
            adminService.getBanHistory(userId, DataService.authToken)
        } catch (e: Exception) {
            Log.e("AdminService", "Erreur lors de la récupération de l'historique", e)
            null
        }
    }

    suspend fun banUser(userId: Long, banRequest: BanRequestDto): Boolean {
        return try {
            val response = adminService.banUser(userId, banRequest, DataService.authToken)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun unbanUser(userId: Long): Boolean {
        return try {
            val response = adminService.unbanUser(userId, DataService.authToken)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getFinancesDashboard(): FinancesDashboardDto? {
        return try {
            adminService.getFinancesDashboard(DataService.authToken)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updatePrices(aiPrice: Double, superLikePrice: Double): Boolean {
        return try {
            adminService.updatePrices(UpdatePricesDto(aiPrice, superLikePrice), DataService.authToken)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun runRedistribution() : Boolean {
        return try {
            adminService.runRedistribution(DataService.authToken)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getRecipes(): List<RecipeDetailDto> {
        val response = adminService.getAllDetailedRecipes(DataService.authToken);
        if (response.isSuccessful) {
            val recipes = response.body() ?: emptyList()
            println("recipes received via GET 'api/recipes' = " +  recipes.size)
            return recipes
        } else {
            println("Error fetching recipes: ${response.code()} - ${response.message()}")
        }
        return emptyList();
    }

    suspend fun deleteRecipe(recipeId: Long): Boolean {
        return try {
            val response = adminService.deleteRecipe(recipeId, DataService.authToken)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
