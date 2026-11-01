package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.PromotionDto
import okhttp3.ResponseBody // <-- AJOUTE CET IMPORT
import retrofit2.Response
import retrofit2.http.*

interface PromotionServiceInterface {

    @GET("api/admin/promotions")
    suspend fun getPromotions(
        @Header("Authorization") token: String
    ): Response<List<PromotionDto>>

    @POST("api/admin/promotions")
    suspend fun createPromotion(
        @Header("Authorization") token: String,
        @Query("type") type: String,
        @Query("promoPrice") promoPrice: Double,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?
    ): Response<ResponseBody>

    @POST("api/admin/promotions/{id}/delete")
    suspend fun deletePromotion(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ResponseBody>
}