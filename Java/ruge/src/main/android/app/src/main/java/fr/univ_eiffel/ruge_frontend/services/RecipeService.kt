package fr.univ_eiffel.ruge_frontend.services

import android.graphics.Bitmap
import com.google.gson.Gson
import fr.univ_eiffel.ruge_frontend.dto.*
import fr.univ_eiffel.ruge_frontend.services.interfaces.RecipeServiceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

object RecipeService {
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(RecipeServiceInterface::class.java)

    suspend fun getRecipes(): List<SimpleRecipeDto> = try {
        val res = api.getAllRecipes()
        if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun getRecipes(page : Int): List<SimpleRecipeDto> {
        return try {
            api.getRecipes(DataService.authToken, page)
        } catch (_: Exception) {
            emptyList()
        }
    }
    // --- PRÉSENT UNE SEULE FOIS ---
    suspend fun searchRecipes(query: String, type: String): List<SimpleRecipeDto> = try {
        val res = api.searchRecipes(query, type, DataService.authToken)
        if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun getRecipeWithInteractions(id: Long, token: String?): RecipeDetailWithInteractionsDto? {
        val auth = if (token != null) "Bearer $token" else null
        val res = api.getRecipeWithInteractions(id, auth)
        return if (res.isSuccessful) res.body() else null
    }

    suspend fun likeRecipe(id: Long, token: String) = api.likeRecipe(id, "Bearer $token").isSuccessful
    suspend fun unlikeRecipe(id: Long, token: String) = api.unlikeRecipe(id, "Bearer $token").isSuccessful
    suspend fun superLikeRecipe(id: Long, token: String) = api.superLikeRecipe(id, "Bearer $token").isSuccessful
    suspend fun followRecipe(id: Long, token: String) = api.followRecipe(id, "Bearer $token").isSuccessful
    suspend fun unfollowRecipe(id: Long, token: String) = api.unfollowRecipe(id, "Bearer $token").isSuccessful
    suspend fun postReview(id: Long, comment: String, token: String) = api.postReview(id, comment, "Bearer $token").isSuccessful

    suspend fun createRecipe(token: String, recipe: RecipeCreateDto, bitmap: Bitmap?): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = RequestBody.create(MediaType.parse("application/json"), Gson().toJson(recipe))
            var imagePart: MultipartBody.Part? = null
            bitmap?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val requestFile = RequestBody.create(MediaType.parse("image/jpeg"), stream.toByteArray())
                imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", requestFile)
            }
            val res = api.createRecipeWithImage("Bearer $token", jsonBody, imagePart)
            if (res.isSuccessful) "OK" else "ERROR"
        } catch (e: Exception) { "ERROR" }
    }

    suspend fun createVariant(recipeId: Long, token: String, variant: RecipeVariantDto): String = try {
        val res = api.createVariant(recipeId, "Bearer $token", variant)
        if (res.isSuccessful) "OK" else "ERROR ${res.code()}"
    } catch (e: Exception) { "ERROR ${e.localizedMessage}" }
}