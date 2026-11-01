package fr.univ_eiffel.ruge_frontend.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared in-memory application state.
 * Can be replaced by DataStore or SharedPreferences for persistence.
 */
object DataService {
    var authToken: String = ""
    var userId: Long? = null
    var userEmail: String? = null
    var userName: String? = null
    var userProfilePictureUrl: String? = null
    var role: String = ""
    var isLoggedIn by mutableStateOf(false)
}
