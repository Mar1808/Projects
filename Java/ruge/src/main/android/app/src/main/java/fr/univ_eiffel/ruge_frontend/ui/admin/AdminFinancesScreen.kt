package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.dto.TransactionDto
import fr.univ_eiffel.ruge_frontend.utils.toReadableDate
import fr.univ_eiffel.ruge_frontend.viewModel.AdminFinancesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinancesScreen(
    navController: NavController,
    viewModel: AdminFinancesViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration — Finances") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.dashboardData != null) {
            val data = viewModel.dashboardData!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "TOTAL DES REDISTRIBUTIONS",
                            value = "${data.totalRedistribution} €",
                            valueColor = Color(0xFFD32F2F)
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "TOTAL DES BÉNÉFICES",
                            value = "${data.totalBenefits} €",
                            valueColor = Color.Black
                        )
                    }
                }

                item {
                    Text(
                        text = "Configuration des Tarifs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = viewModel.aiPriceText,
                                    onValueChange = { viewModel.onAiPriceChanged(it) },
                                    label = { Text("Prix Recherche IA (€)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = viewModel.superLikePriceText,
                                    onValueChange = { viewModel.onSuperLikePriceChanged(it) },
                                    label = { Text("Prix Super Like (€)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.updatePrices() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4B886))
                            ) {
                                Text("Sauvegarder", color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Transactions Récentes & Retraits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.runRedistribution() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Redistribuer tous les super-likes en attente")
                        }
                    }
                }

                item {
                    TransactionFilters(
                        onFilterClick = { max, start, end, source, dest ->
                            viewModel.applyNewFilters(max, start, end, source, dest)
                        },
                        onResetClick = {
                            viewModel.resetFilters()
                        }
                    )
                }

                if (viewModel.filteredTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucune transaction trouvée pour ces filtres.", color = Color.Gray)
                        }
                    }
                } else {
                    items(viewModel.filteredTransactions) { transaction ->
                        TransactionMobileCard(transaction)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun TransactionFilters(
    onFilterClick: (max: Int?, startDate: String, endDate: String, sourceId: Long?, destId: Long?) -> Unit,
    onResetClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    var maxLimit by remember { mutableStateOf("10") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var sourceWalletId by remember { mutableStateOf("") }
    var destWalletId by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Filtres", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filtres de recherche",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(Cliquez pour dérouler/masquer)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = maxLimit,
                        onValueChange = { maxLimit = it },
                        label = { Text("Nombre max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Du") },
                        placeholder = { Text("Ex: 2026-03-27T00:00:00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Au") },
                        placeholder = { Text("Ex: 2026-03-28T23:59:59") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sourceWalletId,
                        onValueChange = { sourceWalletId = it },
                        label = { Text("ID Source Wallet") },
                        placeholder = { Text("Ex: 16") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = destWalletId,
                        onValueChange = { destWalletId = it },
                        label = { Text("ID Destination Wallet") },
                        placeholder = { Text("Ex: 4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onFilterClick(
                                    maxLimit.toIntOrNull(),
                                    startDate,
                                    endDate,
                                    sourceWalletId.toLongOrNull(),
                                    destWalletId.toLongOrNull()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Filtrer") }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(onClick = {
                            maxLimit = "10"
                            startDate = ""
                            endDate = ""
                            sourceWalletId = ""
                            destWalletId = ""
                            onResetClick()
                        }) { Text("Réinitialiser", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).height(80.dp).background(valueColor))
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
fun TransactionMobileCard(transaction: TransactionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = transaction.date.toReadableDate(), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(
                    text = "+${transaction.amount} €",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = transaction.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Source Wallet: ${transaction.sourceUserId}", style = MaterialTheme.typography.labelSmall)
                Text(text = "Dest Wallet: ${transaction.targetUserId}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}