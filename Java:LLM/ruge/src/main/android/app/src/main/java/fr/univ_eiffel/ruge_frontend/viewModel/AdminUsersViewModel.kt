package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.BanRequestDto
import fr.univ_eiffel.ruge_frontend.dto.UserDisplayDto
import fr.univ_eiffel.ruge_frontend.services.AdminService
import kotlinx.coroutines.launch

class AdminUsersViewModel : ViewModel() {
    private var allUsers = emptyList<UserDisplayDto>()
    var users by mutableStateOf<List<UserDisplayDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            isLoading = true
            allUsers = AdminService.getUsers()
            applyFilter()
            isLoading = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        users = if (searchQuery.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun banUser(userId: Long, reason: String, duration: Int) {
        viewModelScope.launch {
            val banRequest = BanRequestDto(reason, duration)
            println(banRequest)
            val isSuccess = AdminService.banUser(userId, banRequest)
            println(isSuccess)
            if (isSuccess) {
                loadUsers()
            } else {
                println("Error while banning user.")
            }
        }
    }

    fun unbanUser(userId: Long) {
        viewModelScope.launch {
            val isSuccess = AdminService.unbanUser(userId)
            if (isSuccess) {
                loadUsers()
            } else {
                println("Error while unbanning user.")
            }
        }
    }
}