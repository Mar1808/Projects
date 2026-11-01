package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.UserStatsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface UserStatsInterface {

    @GET("profile/{id}/stats")
    suspend fun getUserStats(
        @Path("id") userId: Long,
        @Header("Authorization") token: String
    ): Response<UserStatsResponse>
}