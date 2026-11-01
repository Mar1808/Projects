package fr.univ_eiffel.ruge_frontend.ui.account

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.services.NotificationService
import fr.univ_eiffel.ruge_frontend.ui.layout.Footer
import fr.univ_eiffel.ruge_frontend.ui.layout.Header
import fr.univ_eiffel.ruge_frontend.ui.theme.Beige1
import fr.univ_eiffel.ruge_frontend.ui.theme.Ruge
import fr.univ_eiffel.ruge_frontend.viewModel.UserViewModel

@Composable
fun LoginScreen(navController: NavController, userViewModel: UserViewModel = viewModel()) {
    val context = LocalContext.current

    // 1. Initialisation du service localement (Ne perturbe pas le reste de l'app)
    val notificationService = remember { NotificationService(context) }

    // 2. Gestionnaire de permission (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Optionnel : Gérer le refus */ }
    )

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val errorMessage = userViewModel.errorMessage
    // Demande la permission pour les notifications au lancement de l'écran
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Gestion de la réussite de connexion
    LaunchedEffect(DataService.isLoggedIn) {
        if (DataService.isLoggedIn) {
            // SÉCURITÉ : On empêche un plantage de la notif de casser la navigation
            try {
                notificationService.showSimpleNotification(
                    title = "Connexion réussie",
                    message = "Vous êtes maintenant connecté à RUGE"
                )
            } catch (e: Exception) {
                println("Erreur de notification: ${e.message}")
            }

            navController.navigate("Home") {
                popUpTo("Login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(modifier = Modifier, navController = navController)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connexion",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Beige1, shape = RoundedCornerShape(15.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { userViewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ruge)
                ) {
                    Text("Valider la connexion", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Footer(modifier = Modifier, navController = navController, currentRoute = "Login")
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    LoginScreen(navController = rememberNavController())
}