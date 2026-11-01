package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.dto.UserBanHistoryDto
import fr.univ_eiffel.ruge_frontend.services.AdminService
import fr.univ_eiffel.ruge_frontend.utils.toReadableDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBanHistoryScreen(navController: NavController, userId: Long) {
    var logs by remember { mutableStateOf<UserBanHistoryDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        logs = AdminService.getBanHistory(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des sanctions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (logs == null || logs?.history?.isEmpty() == true) {
                Text("Aucun historique de bannissement pour cet utilisateur.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs?.history ?: emptyList()) { log ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Date : ${log.banDate.toReadableDate()}", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Raison : ${log.reason}", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Durée : ${log.duration} jours", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}