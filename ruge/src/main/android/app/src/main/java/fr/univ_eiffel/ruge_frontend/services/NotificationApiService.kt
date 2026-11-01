package fr.univ_eiffel.ruge_frontend.services

import fr.univ_eiffel.ruge_frontend.dto.NotificationDto
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NotificationApiService {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(NotificationApi::class.java)

    suspend fun getLatestNotifications(token: String): Response<List<NotificationDto>>? {
        return try {
            api.getLatest(token)
        } catch (e: Exception) {
            null
        }
    }
}