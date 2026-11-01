package fr.univ_eiffel.ruge_frontend.ui.account

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.viewModel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = viewModel() // C'est ici qu'on instancie le ViewModel très simplement
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Variables locales pour les champs de texte
    var rechargeAmount by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }

    // Afficher un Toast si on a un message d'erreur ou de succès
    LaunchedEffect(viewModel.messageInfo) {
        viewModel.messageInfo?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // Ouvrir le navigateur si on reçoit une URL Stripe
    LaunchedEffect(viewModel.urlToOpen) {
        viewModel.urlToOpen?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.urlHasBeenOpened()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon wallet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Si c'est en train de charger, on affiche la petite roue
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Si on a bien reçu les infos du wallet
            viewModel.walletInfo?.let { info ->
                
                // --- 1. CARTE DU SOLDE ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Solde actuel", style = MaterialTheme.typography.titleMedium)
                        Text("${info.walletBalance} EUR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // --- 2. CARTE RECHARGER ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recharger votre wallet", style = MaterialTheme.typography.titleMedium)
                        
                        OutlinedTextField(
                            value = rechargeAmount,
                            onValueChange = { rechargeAmount = it },
                            label = { Text("Montant (EUR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { viewModel.rechargeWallet(rechargeAmount) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Recharger votre wallet")
                        }
                    }
                }

                // --- 3. CARTE RETIRER ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Retirer vers votre banque", style = MaterialTheme.typography.titleMedium)
                        
                        OutlinedTextField(
                            value = withdrawAmount,
                            onValueChange = { withdrawAmount = it },
                            label = { Text("Montant à retirer (EUR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { viewModel.withdrawFunds(withdrawAmount) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retirer vers mon compte bancaire")
                        }
                    }
                }
            } ?: run {
                // Si walletInfo est null et qu'on ne charge pas, c'est qu'il y a eu une erreur de chargement
                if (!viewModel.isLoading) {
                    Button(onClick = { viewModel.loadWallet() }) {
                        Text("Réessayer de charger")
                    }
                }
            }
        }
    }
}