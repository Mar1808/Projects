package fr.univ_eiffel.ruge_frontend.services

import fr.univ_eiffel.ruge_frontend.services.interfaces.WalletServiceInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object WalletService {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    private val walletApi = retrofit.create<WalletServiceInterface>()

    suspend fun getWalletInfo() = try {
        walletApi.getWalletInfo(DataService.authToken)
    } catch (e: Exception) {
        println("Erreur Service getWalletInfo: ${e.message}")
        null
    }

    suspend fun createCheckout(amount: String) = try {
        walletApi.createCheckout(DataService.authToken, amount)
    } catch (e: Exception) {
        println("Erreur Service createCheckout: ${e.message}")
        null
    }

    suspend fun withdrawToBank(amount: String) = try {
        walletApi.withdrawToBank(DataService.authToken, amount)
    } catch (e: Exception) {
        println("Erreur Service withdrawToBank: ${e.message}")
        null
    }
}