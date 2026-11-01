package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RecipeServiceInterface {
    @GET("recipes")
    suspend fun getAllRecipes(): Response<List<SimpleRecipeDto>>

    @GET("recipes/search")
    suspend fun searchRecipes(@Query("query") query: String): Response<List<SimpleRecipeDto>>

    @GET("recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String?,
        @Query("page") page: Int = 0
    ): List<SimpleRecipeDto>

    @GET("recipes/search")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("type") type: String,
        @Header("Authorization") token: String?
    ): Response<List<SimpleRecipeDto>>

    @GET("recipes/{id}")
    suspend fun getRecipeWithInteractions(
        @Path("id") id: Long,
        @Header("Authorization") token: String?
    ): Response<RecipeDetailWithInteractionsDto>

    @POST("recipes/{id}/like")
    suspend fun likeRecipe(@Path("id") id: Long, @Header("Authorization") token: String): Response<ResponseBody>

    @POST("recipes/{id}/unlike")
    suspend fun unlikeRecipe(@Path("id") id: Long, @Header("Authorization") token: String): Response<ResponseBody>

    @POST("recipes/{id}/follow")
    suspend fun followRecipe(@Path("id") id: Long, @Header("Authorization") token: String): Response<ResponseBody>

    @POST("recipes/{id}/unfollow")
    suspend fun unfollowRecipe(@Path("id") id: Long, @Header("Authorization") token: String): Response<ResponseBody>

    @POST("recipes/{id}/super-like")
    suspend fun superLikeRecipe(
        @Path("id") id: Long,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("recipes/{id}/reviews")
    suspend fun postReview(@Path("id") id: Long, @Query("review") comment: String, @Header("Authorization") token: String): Response<ResponseBody>

    @Multipart
    @POST("recipes")
    suspend fun createRecipeWithImage(
        @Header("Authorization") token: String,
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<ResponseBody>

    @POST("recipes/{id}/variant")
    suspend fun createVariant(@Path("id") id: Long, @Header("Authorization") token: String, @Body variant: RecipeVariantDto): Response<ResponseBody>
}