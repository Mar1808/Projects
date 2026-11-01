package fr.univ_eiffel.ruge_frontend.ui.admin // Ajusté si besoin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import fr.univ_eiffel.ruge_frontend.dto.PromotionDto
import fr.univ_eiffel.ruge_frontend.utils.toReadableDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionCard(
    title: String,
    typeOptions: List<String> = listOf("Super-Like (Réduction)", "IA (Réduction)", "Super-Like (Xème offert)"),
    priceLabel: String = "Prix promo (€)",
    pricePlaceholder: String = "Ex: 0.50",
    buttonText: String = "Planifier la promotion",
    onPlanifier: (String, String, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(typeOptions.first()) }
    var expanded by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type de promo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    typeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedType = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text(priceLabel) },
                placeholder = { Text(pricePlaceholder) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Début (Ex: 2026-03-27T19:00:00)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("Fin (Ex: 2026-03-28T19:00:00)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onPlanifier(selectedType, priceText, startDate, endDate) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4B886)),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@Composable
fun StatusBadge(active: Boolean) {
    val backgroundColor = if (!active) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val textColor = if (!active) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val text = if (!active) "INACTIVE" else "ACTIVE"

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PromotionItemCard(
    promotion: PromotionDto,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = promotion.type,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(active = promotion.active)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format("%.2f", promotion.promoPrice)} €",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Début : ${promotion.startDate.toReadableDate()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Fin : ${promotion.endDate.toReadableDate()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Supprimer", color = Color.White)
                }
            }
        }
    }
}