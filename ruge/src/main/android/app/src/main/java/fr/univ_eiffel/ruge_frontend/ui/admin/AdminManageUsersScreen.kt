package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.dto.UserDisplayDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUsersScreen(
    navController: NavController,
    viewModel: AdminUsersViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des Utilisateurs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Rechercher un utilisateur") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (viewModel.users.isEmpty()) {
                    Text(
                        text = if (viewModel.searchQuery.isNotEmpty()) "Aucun résultat pour '${viewModel.searchQuery}'." else "Aucun utilisateur trouvé.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.users) { user ->
                            UserRow(
                                user = user,
                                onShowHistory = { userId ->
                                    navController.navigate("AdminBanHistory/$userId")
                                },
                                onBan = { reason, duration ->
                                    viewModel.banUser(user.id, reason, duration)
                                },
                                onUnban = {
                                    viewModel.unbanUser(user.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRow(
    user: UserDisplayDto,
    onShowHistory: (userId: Long) -> Unit,
    onBan: (reason: String, duration: Int) -> Unit,
    onUnban: () -> Unit
) {
    var showBanDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.name, style = MaterialTheme.typography.titleMedium)
            Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                Text(text = user.role, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            val badgeColor = if (user.status == "ACTIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Badge(containerColor = badgeColor) {
                Text(text = user.status, modifier = Modifier.padding(horizontal = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { onShowHistory(user.id) }) {
                    Text("Historique")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (user.status == "ACTIVE") {
                    Button(
                        onClick = { showBanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Bannir")
                    }
                } else {
                    Button(
                        onClick = { onUnban() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Débannir")
                    }
                }
            }
        }
    }

    if (showBanDialog) {
        var reason by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            title = { Text("Bannir ${user.name}") },
            text = {
                Column {
                    Text("Veuillez indiquer la raison et la durée du bannissement.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Raison (ex: Spam, Insultes...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Durée (en jours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val durationInt = duration.toIntOrNull() ?: 0
                        onBan(reason, durationInt)
                        showBanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmer le ban")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}