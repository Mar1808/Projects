package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.BanRequestDto
import fr.univ_eiffel.ruge_frontend.dto.FinancesDashboardDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeDetailDto
import fr.univ_eiffel.ruge_frontend.dto.UpdatePricesDto
import fr.univ_eiffel.ruge_frontend.dto.UserBanHistoryDto
import fr.univ_eiffel.ruge_frontend.dto.UserDisplayDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import kotlin.String

interface AdminServiceInterface {
    @GET("users")
    suspend fun getUsers(@Header("Authorization") token : String): List<UserDisplayDto>

    @GET("users/{userId}/ban-history")
    suspend fun getBanHistory(@Path("userId") userId: Long, @Header("Authorization") token : String): UserBanHistoryDto

    @POST("users/{userId}/ban")
    suspend fun banUser(@Path("userId") userId: Long, @Body request: BanRequestDto, @Header("Authorization") token : String): Response<Unit>

    @POST("users/{userId}/unban")
    suspend fun unbanUser(@Path("userId") userId: Long, @Header("Authorization") token : String): Response<Unit>


    @GET("finances/dashboard")
    suspend fun getFinancesDashboard(@Header("Authorization") token : String): FinancesDashboardDto

    @POST("finances/update-prices")
    suspend fun updatePrices(
        @Body updatePricesDto: UpdatePricesDto,
        @Header("Authorization") token: String
    ): Boolean

    @POST("finances/redistribution")
    suspend fun runRedistribution(@Header("Authorization") token : String): Boolean

    @GET("recipes/allDetailedRecipes")
    suspend fun getAllDetailedRecipes(@Header("Authorization") token : String): Response<List<RecipeDetailDto>>

    @POST("recipes/{recipeId}/delete")
    suspend fun deleteRecipe(@Path("recipeId") recipeId: Long, @Header("Authorization") token : String): Response<Boolean>
}
