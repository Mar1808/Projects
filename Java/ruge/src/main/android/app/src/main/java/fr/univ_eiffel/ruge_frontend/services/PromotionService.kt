package fr.univ_eiffel.ruge_frontend.services

import fr.univ_eiffel.ruge_frontend.services.interfaces.PromotionServiceInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object PromotionService {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    private val promotionApi = retrofit.create<PromotionServiceInterface>()

    suspend fun getPromotions() = try {
        promotionApi.getPromotions(DataService.authToken)
    } catch (e: Exception) {
        println("Erreur Service getPromotions: ${e.message}")
        null
    }

    suspend fun createPromotion(
        type: String,
        promoPrice: Double,
        startDate: String?,
        endDate: String?
    ) = try {
        promotionApi.createPromotion(DataService.authToken, type, promoPrice, startDate, endDate)
    } catch (e: Exception) {
        println("Erreur Service createPromotion: ${e.message}")
        null
    }

    suspend fun deletePromotion(id: Long) = try {
        println("Deleting promotion with id: $id using token: ${DataService.authToken}")
        promotionApi.deletePromotion(DataService.authToken, id)
    } catch (e: Exception) {
        println("Erreur Service deletePromotion: ${e.message}")
        null
    }
}