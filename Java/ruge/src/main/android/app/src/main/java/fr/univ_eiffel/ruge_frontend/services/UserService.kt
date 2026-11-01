package fr.univ_eiffel.ruge_frontend.services

import fr.univ_eiffel.ruge_frontend.dto.LoginDTO
import fr.univ_eiffel.ruge_frontend.dto.SimpleRecipeDto
import fr.univ_eiffel.ruge_frontend.dto.UserDTO
import fr.univ_eiffel.ruge_frontend.dto.UserStatsResponse
import fr.univ_eiffel.ruge_frontend.services.interfaces.UserConnectionService
import fr.univ_eiffel.ruge_frontend.services.interfaces.UserStatsInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object UserService {
    private const val BASE_URL = "http://10.0.2.2:8080/api/users/"
    private val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BASE_URL).build()
    private val service =  retrofit.create<UserConnectionService>()
    private val serviceStats =  retrofit.create<UserStatsInterface>()

    suspend fun getUserStats(userId: Long?): UserStatsResponse? {
        requireNotNull(userId) { "userId ne peut pas être null" }

        val response = serviceStats.getUserStats(userId, "Bearer ${DataService.authToken}")
        if (response.isSuccessful) {
            return response.body();
        }
        return null
    }

    suspend fun createUser(username:String, email: String, password: String): String {
        try {
            val response = service.registerUser(UserDTO(email, password, username, "", 0))
            return if (response.isSuccessful) {
                "OK"
            } else {
                "ERROR"
            }
        } catch (e: Exception) {
            return "Error: ${e.localizedMessage}"
        }
    }

    suspend fun loginUser(email: String, password: String): String? {
        try {
            val response = service.loginUser(LoginDTO(email, password))
            if (response.isSuccessful) {
                val authResponse = response.body()
                DataService.authToken = authResponse?.token.toString()
                DataService.role = authResponse?.role.toString()
                DataService.userName = authResponse?.name
                DataService.userEmail = authResponse?.email
                DataService.userProfilePictureUrl = authResponse?.imageUrl
                DataService.userId = authResponse?.userId
                DataService.isLoggedIn = true
                return null
            } else {
                val errorCode = response.code()
                if (errorCode == 403) {
                    return "Votre compte a été banni."
                }
                if (errorCode == 401 || errorCode == 400) {
                    return "Email ou mot de passe incorrect."
                }
                return "Erreur lors de la connexion (Code: $errorCode)."
            }
        } catch (_: Exception) {
            return "Une erreur de connexion est survenue. Veuillez réessayer."
        }
    }

    suspend fun loadMyRecipes(token : String) : List<SimpleRecipeDto> {
        try {
            val response = service.getMyRecipes(token);
            if (response.isSuccessful) {
                val recipes = response.body() ?: emptyList()
                println("recipes received via POST 'api/users/profile/my-recipes' = " + recipes.size)
                return recipes
            } else {
                println("Error fetching my recipes: ${response.code()} - ${response.message()}")
                return emptyList()
            }
        } catch (e : Exception) {
            println("Exception fetching my recipes: ${e.localizedMessage}")
            return emptyList()
        }
    }
}
