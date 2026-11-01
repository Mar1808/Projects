package fr.univ_eiffel.ruge_frontend.services.interfaces

import fr.univ_eiffel.ruge_frontend.dto.AuthResponseDTO
import fr.univ_eiffel.ruge_frontend.dto.LoginDTO
import fr.univ_eiffel.ruge_frontend.dto.SimpleRecipeDto
import fr.univ_eiffel.ruge_frontend.dto.UserDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface UserConnectionService {
    @POST("register")
    suspend fun registerUser(@Body userDTO: UserDTO) : Response<ResponseBody>

    @POST("login")
    suspend fun loginUser(@Body loginDTO: LoginDTO) : Response<AuthResponseDTO>

    @GET("profile/my-recipes")
    suspend fun getMyRecipes(@Header("Authorization") token : String) : Response<List<SimpleRecipeDto>>
}