package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.*
import retrofit2.Response
import retrofit2.http.*

interface WalletServiceInterface {

    @GET("api/users/wallet")
    suspend fun getWalletInfo(@Header("Authorization") token: String): Response<WalletInfoDto>

    @POST("api/users/wallet/checkout")
    suspend fun createCheckout(
        @Header("Authorization") token: String,
        @Query("amount") amount: String
    ): Response<CheckoutResponseDto>

    @POST("api/users/wallet/withdraw")
    suspend fun withdrawToBank(
        @Header("Authorization") token: String,
        @Query("amount") amount: String
    ): Response<MessageResponseDto>
}