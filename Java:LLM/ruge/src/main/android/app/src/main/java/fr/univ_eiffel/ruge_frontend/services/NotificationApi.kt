package fr.univ_eiffel.ruge_frontend.services

import fr.univ_eiffel.ruge_frontend.dto.NotificationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface NotificationApi {
    @GET("api/notifications/latest")
    suspend fun getLatest(@Header("Authorization") token: String): Response<List<NotificationDto>>
}