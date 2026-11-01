package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.viewModel.PromotionViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPromotionsScreen(
    navController: NavController,
    viewModel: PromotionViewModel = viewModel() // Injection du ViewModel
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadPromotions()
            delay(10000) // Attend 10 secondes avant le prochain refresh
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration — Promotions") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- CRÉATION ---
            PromotionCard(
                title = "Créer une Promotion",
                onPlanifier = { type, prix, debut, fin ->
                    val priceDouble = prix.toDoubleOrNull() ?: 0.0
                    // On délègue la création au ViewModel
                    viewModel.createPromotion(type, priceDouble, debut, fin)
                }
            )

            HorizontalDivider()

            // --- LISTE ---
            if (viewModel.isLoading && viewModel.promotions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                viewModel.promotions.forEach { promo ->
                    PromotionItemCard(
                        promotion = promo,
                        onDelete = {
                            viewModel.deletePromotion(promo.id)
                        }
                    )
                }
            }
        }
    }
}