package fr.univ_eiffel.ruge_frontend.ui.recipe

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.dto.RecipeCreateDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeCreateIngredientDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeCreateInstructionDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.ui.layout.Footer
import fr.univ_eiffel.ruge_frontend.ui.layout.Header
import fr.univ_eiffel.ruge_frontend.viewModel.RecipeViewModel

private val BgColor = Color(0xFFFAF8F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val PrimaryColor = Color(0xFFE64A19)
private val PrimaryDark = Color(0xFFC73D12)
private val PrimaryLight = Color(0xFFFFF2EE)
private val BorderColor = Color(0xFFE8E0D5)
private val TextMain = Color(0xFF1E1E1E)
private val TextMuted = Color(0xFF888888)
private val DangerBg = Color(0xFFFDE8E6)
private val DangerText = Color(0xFFB23A33)
private val WarningBg = Color(0xFFFFF5E6)
private val WarningText = Color(0xFF9A5B00)

@Composable
fun CreateRecipeScreen(
    navController: NavController,
    viewModel: RecipeViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var totalDuration by remember { mutableStateOf("") }

    var ingredientName by remember { mutableStateOf("") }
    var ingredientQuantity by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<RecipeCreateIngredientDto>() }

    var stepText by remember { mutableStateOf("") }
    var stepDuration by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<RecipeCreateInstructionDto>() }

    var localError by remember { mutableStateOf<String?>(null) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryColor,
        unfocusedBorderColor = BorderColor,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = PrimaryDark
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap: Bitmap? ->
            capturedPhoto = bitmap
        }
    )

    val createStatus = viewModel.createStatus
    LaunchedEffect(createStatus) {
        if (createStatus == "OK") {
            viewModel.clearCreateStatus()
            navController.navigate("MyAccount")
        }
    }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            Footer(modifier = Modifier, navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(modifier = Modifier, navController = navController)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Creer une recette",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ajoutez la photo, les ingredients et les etapes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionCard(
                    title = "Informations",
                    icon = Icons.Default.Restaurant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .background(PrimaryLight, RoundedCornerShape(16.dp))
                            .clickable { cameraLauncher.launch(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedPhoto != null) {
                            Image(
                                bitmap = capturedPhoto!!.asImageBitmap(),
                                contentDescription = "Photo capturee",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Prendre une photo",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom de la recette") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL image (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = totalDuration,
                        onValueChange = { totalDuration = it },
                        label = { Text("Duree totale (min)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionCard(
                    title = "Ingredients",
                    icon = Icons.Default.ShoppingBasket
                ) {
                    OutlinedTextField(
                        value = ingredientName,
                        onValueChange = { ingredientName = it },
                        label = { Text("Nom de l'ingredient") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ingredientQuantity,
                        onValueChange = { ingredientQuantity = it },
                        label = { Text("Quantite") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val safeName = ingredientName.trim()
                            val safeQuantity = ingredientQuantity.trim()
                            if (safeName.isNotEmpty() && safeQuantity.isNotEmpty()) {
                                ingredients.add(RecipeCreateIngredientDto(safeName, safeQuantity))
                                ingredientName = ""
                                ingredientQuantity = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Ajouter un ingredient")
                    }

                    if (ingredients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ingredients.forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BgColor,
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.name} - ${item.quantity}",
                                        color = TextMain,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { ingredients.removeAt(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = DangerText
                                        )
                                        Text(text = "Retirer", color = DangerText)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionCard(
                    title = "Etapes",
                    icon = Icons.Default.FormatListNumbered
                ) {
                    OutlinedTextField(
                        value = stepText,
                        onValueChange = { stepText = it },
                        label = { Text("Etape") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stepDuration,
                        onValueChange = { stepDuration = it },
                        label = { Text("Duree (min)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val safeStep = stepText.trim()
                            val safeDuration = stepDuration.trim().toIntOrNull()
                            if (safeStep.isNotEmpty() && safeDuration != null) {
                                steps.add(RecipeCreateInstructionDto(safeStep, safeDuration))
                                stepText = ""
                                stepDuration = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Ajouter une etape")
                    }

                    if (steps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        steps.forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BgColor,
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.step} (${item.duration} min)",
                                        color = TextMain,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { steps.removeAt(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = DangerText
                                        )
                                        Text(text = "Retirer", color = DangerText)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionCard(
                    title = "Publication",
                    icon = Icons.Default.Restaurant
                ) {
                    val backendError =
                        if (createStatus != null && createStatus != "OK" && createStatus != "LOADING") {
                            createStatus
                        } else {
                            null
                        }

                    if (!DataService.isLoggedIn) {
                        MessageBanner(
                            text = "Connexion requise pour creer une recette",
                            containerColor = WarningBg,
                            contentColor = WarningText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            val durationValue = totalDuration.trim().toIntOrNull()
                            localError = when {
                                name.trim().isEmpty() -> "Le nom est requis"
                                durationValue == null -> "La duree totale doit etre un nombre"
                                ingredients.isEmpty() -> "Ajoutez au moins un ingredient"
                                steps.isEmpty() -> "Ajoutez au moins une etape"
                                else -> null
                            }
                            if (localError == null && durationValue != null && DataService.isLoggedIn) {
                                val request = RecipeCreateDto(
                                    name = name.trim(),
                                    description = description.trim(),
                                    imageUrl = imageUrl.trim(),
                                    totalDuration = durationValue,
                                    ingredients = ingredients.toList(),
                                    steps = steps.toList()
                                )
                                viewModel.createRecipe(request, capturedPhoto)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(999.dp),
                        enabled = DataService.isLoggedIn && createStatus != "LOADING"
                    ) {
                        if (createStatus == "LOADING") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Creer la recette",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        MessageBanner(
                            text = localError ?: "",
                            containerColor = DangerBg,
                            contentColor = DangerText
                        )
                    }
                    if (backendError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        MessageBanner(
                            text = backendError,
                            containerColor = DangerBg,
                            contentColor = DangerText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(PrimaryLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun MessageBanner(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateRecipeScreenPreview() {
    CreateRecipeScreen(navController = rememberNavController())
}