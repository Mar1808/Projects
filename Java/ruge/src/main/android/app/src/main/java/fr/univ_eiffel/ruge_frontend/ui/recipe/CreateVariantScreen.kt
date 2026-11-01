package fr.univ_eiffel.ruge_frontend.ui.recipe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.TextFieldColors
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.dto.RecipeVariantDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeVariantIngredientDto
import fr.univ_eiffel.ruge_frontend.dto.RecipeVariantStepDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.ui.layout.Footer
import fr.univ_eiffel.ruge_frontend.ui.layout.Header
import fr.univ_eiffel.ruge_frontend.viewModel.RecipeViewModel

private const val DeletedPrefix = "__DELETED__"
private const val KeepedPrefix = "__KEEPED__"

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
private val SuccessBg = Color(0xFFE8F6ED)
private val SuccessText = Color(0xFF1F6539)
private val WarningBg = Color(0xFFFFF5E6)
private val WarningText = Color(0xFF9A5B00)

private data class VariantStepItem(
    val step: String,
    val duration: Int,
    val original: Boolean,
    val deleted: Boolean
)

private data class VariantIngredientItem(
    val name: String,
    val quantity: String,
    val original: Boolean,
    val deleted: Boolean
)

@Composable
fun CreateVariantScreen(
    navController: NavController,
    recipeId: Long,
    viewModel: RecipeViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var ingredientName by remember { mutableStateOf("") }
    var ingredientQuantity by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<VariantIngredientItem>() }

    var stepText by remember { mutableStateOf("") }
    var stepDuration by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<VariantStepItem>() }

    var localError by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }

    val recipe = viewModel.recipeDetail
    val isLoading = viewModel.isLoading
    val createStatus = viewModel.createVariantStatus
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryColor,
        unfocusedBorderColor = BorderColor,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = PrimaryDark
    )

    val moveStep: (Int, Int) -> Unit = { from, to ->
        if (to in steps.indices) {
            val item = steps.removeAt(from)
            steps.add(to, item)
        }
    }

    LaunchedEffect(recipeId) {
        viewModel.clearCreateVariantStatus()
        viewModel.loadRecipeById(recipeId)
    }

    LaunchedEffect(recipe) {
        if (recipe != null && !initialized) {
            name = if (recipe.name.isNotBlank()) "${recipe.name} (variante)" else "Variante"
            description = recipe.description
            recipe.steps
                .filterNot { it.step.startsWith(DeletedPrefix) }
                .forEach { step ->
                    steps.add(
                        VariantStepItem(
                            step = step.step.removePrefix(KeepedPrefix),
                            duration = step.duration,
                            original = true,
                            deleted = false
                        )
                    )
                }
            recipe.ingredients
                .filterNot { it.name.startsWith(DeletedPrefix) }
                .forEach { ing ->
                    ingredients.add(
                        VariantIngredientItem(
                            name = ing.name.removePrefix(KeepedPrefix),
                            quantity = ing.quantity,
                            original = true,
                            deleted = false
                        )
                    )
                }
            initialized = true
        }
    }

    LaunchedEffect(createStatus) {
        if (createStatus == "OK") {
            viewModel.clearCreateVariantStatus()
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
                            text = "Creer une variante",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (recipe != null) {
                                "Base: ${recipe.name}"
                            } else {
                                "Chargement de la recette source..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Modifiez, reordonnez, supprimez ou ajoutez des elements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading && recipe == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceColor,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryColor,
                                strokeWidth = 2.dp
                            )
                            Text("Chargement de la recette...", color = TextMuted)
                        }
                    }
                }

                if (!isLoading && recipe == null) {
                    MessageBanner(
                        text = "Recette introuvable.",
                        containerColor = DangerBg,
                        contentColor = DangerText
                    )
                }

                if (recipe != null) {
                    VariantInfoSection(
                        name = name,
                        onNameChange = { name = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VariantIngredientsSection(
                        ingredientName = ingredientName,
                        onIngredientNameChange = { ingredientName = it },
                        ingredientQuantity = ingredientQuantity,
                        onIngredientQuantityChange = { ingredientQuantity = it },
                        ingredients = ingredients,
                        onAddIngredient = {
                            val safeName = ingredientName.trim()
                            if (safeName.isNotEmpty()) {
                                ingredients.add(
                                    VariantIngredientItem(
                                        name = safeName,
                                        quantity = ingredientQuantity.trim(),
                                        original = false,
                                        deleted = false
                                    )
                                )
                                ingredientName = ""
                                ingredientQuantity = ""
                            }
                        },
                        onToggleIngredient = { index ->
                            val item = ingredients[index]
                            ingredients[index] = item.copy(deleted = !item.deleted)
                        },
                        onRemoveIngredient = { index ->
                            ingredients.removeAt(index)
                        },
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VariantStepsSection(
                        stepText = stepText,
                        onStepTextChange = { stepText = it },
                        stepDuration = stepDuration,
                        onStepDurationChange = { stepDuration = it },
                        steps = steps,
                        onAddStep = {
                            val safeStep = stepText.trim()
                            val safeDuration = stepDuration.trim().toIntOrNull()
                            if (safeStep.isNotEmpty() && safeDuration != null) {
                                steps.add(
                                    VariantStepItem(
                                        step = safeStep,
                                        duration = safeDuration,
                                        original = false,
                                        deleted = false
                                    )
                                )
                                stepText = ""
                                stepDuration = ""
                            }
                        },
                        onMoveStep = moveStep,
                        onUpdateStep = { index, step ->
                            val item = steps[index]
                            val shouldDropOriginal = item.original && step != item.step
                            steps[index] = item.copy(
                                step = step,
                                original = if (shouldDropOriginal) false else item.original
                            )
                        },
                        onUpdateStepDuration = { index, value ->
                            val item = steps[index]
                            val parsed = value.toIntOrNull() ?: 0
                            val shouldDropOriginal = item.original && parsed != item.duration
                            steps[index] = item.copy(
                                duration = parsed,
                                original = if (shouldDropOriginal) false else item.original
                            )
                        },
                        onToggleStep = { index ->
                            val item = steps[index]
                            steps[index] = item.copy(deleted = !item.deleted)
                        },
                        onRemoveStep = { index ->
                            steps.removeAt(index)
                        },
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VariantPublicationSection(
                        sourceRecipeId = recipeId,
                        name = name,
                        description = description,
                        ingredients = ingredients,
                        steps = steps,
                        localError = localError,
                        onLocalErrorChange = { localError = it },
                        createStatus = createStatus,
                        onCreateVariant = { request ->
                            viewModel.createVariant(recipeId, request)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VariantInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    fieldColors: TextFieldColors
) {
    SectionCard(
        title = "Informations",
        icon = Icons.Default.Restaurant
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Titre de la variante") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = fieldColors
        )
    }
}

@Composable
private fun VariantIngredientsSection(
    ingredientName: String,
    onIngredientNameChange: (String) -> Unit,
    ingredientQuantity: String,
    onIngredientQuantityChange: (String) -> Unit,
    ingredients: List<VariantIngredientItem>,
    onAddIngredient: () -> Unit,
    onToggleIngredient: (Int) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    fieldColors: TextFieldColors
) {
    SectionCard(
        title = "Ingredients",
        icon = Icons.Default.ShoppingBasket
    ) {
        OutlinedTextField(
            value = ingredientName,
            onValueChange = onIngredientNameChange,
            label = { Text("Nom de l'ingredient") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ingredientQuantity,
            onValueChange = onIngredientQuantityChange,
            label = { Text("Quantite") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddIngredient,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text("Ajouter un ingredient")
        }

        if (ingredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ingredients.forEachIndexed { index, item ->
                val textColor = if (item.deleted) TextMuted else TextMain
                val decoration = if (item.deleted) TextDecoration.LineThrough else TextDecoration.None
                val rowBg = if (item.deleted) DangerBg else BgColor
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = rowBg,
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
                            color = textColor,
                            textDecoration = decoration,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.original) {
                            val label = if (item.deleted) "Restaurer" else "Supprimer"
                            val icon = if (item.deleted) Icons.Default.Restore else Icons.Default.Delete
                            val color = if (item.deleted) SuccessText else DangerText
                            TextButton(onClick = { onToggleIngredient(index) }) {
                                Icon(icon, contentDescription = null, tint = color)
                                Text(label, color = color)
                            }
                        } else {
                            TextButton(onClick = { onRemoveIngredient(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = DangerText
                                )
                                Text("Retirer", color = DangerText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantStepsSection(
    stepText: String,
    onStepTextChange: (String) -> Unit,
    stepDuration: String,
    onStepDurationChange: (String) -> Unit,
    steps: List<VariantStepItem>,
    onAddStep: () -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onUpdateStep: (Int, String) -> Unit,
    onUpdateStepDuration: (Int, String) -> Unit,
    onToggleStep: (Int) -> Unit,
    onRemoveStep: (Int) -> Unit,
    fieldColors: TextFieldColors
) {
    SectionCard(
        title = "Etapes",
        icon = Icons.Default.FormatListNumbered
    ) {
        OutlinedTextField(
            value = stepText,
            onValueChange = onStepTextChange,
            label = { Text("Etape") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = stepDuration,
            onValueChange = onStepDurationChange,
            label = { Text("Duree (min)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddStep,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text("Ajouter une etape")
        }

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            steps.forEachIndexed { index, item ->
                val textColor = if (item.deleted) TextMuted else TextMain
                val decoration = if (item.deleted) TextDecoration.LineThrough else TextDecoration.None
                val canMoveUp = index > 0
                val canMoveDown = index < steps.lastIndex
                val rowBg = if (item.deleted) DangerBg else BgColor

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = rowBg,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Etape ${index + 1}",
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = item.step,
                            onValueChange = { onUpdateStep(index, it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            enabled = !item.deleted,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                textDecoration = decoration
                            ),
                            colors = fieldColors
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = item.duration.toString(),
                            onValueChange = { onUpdateStepDuration(index, it) },
                            label = { Text("Duree (min)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !item.deleted,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { onMoveStep(index, index - 1) },
                                    enabled = canMoveUp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null
                                    )
                                    Text("Monter")
                                }
                                TextButton(
                                    onClick = { onMoveStep(index, index + 1) },
                                    enabled = canMoveDown
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                    Text("Descendre")
                                }
                            }
                            if (item.original) {
                                val label = if (item.deleted) "Restaurer" else "Supprimer"
                                val icon = if (item.deleted) Icons.Default.Restore else Icons.Default.Delete
                                val color = if (item.deleted) SuccessText else DangerText
                                TextButton(onClick = { onToggleStep(index) }) {
                                    Icon(icon, contentDescription = null, tint = color)
                                    Text(label, color = color)
                                }
                            } else {
                                TextButton(onClick = { onRemoveStep(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = DangerText
                                    )
                                    Text("Retirer", color = DangerText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantPublicationSection(
    sourceRecipeId: Long,
    name: String,
    description: String,
    ingredients: List<VariantIngredientItem>,
    steps: List<VariantStepItem>,
    localError: String?,
    onLocalErrorChange: (String?) -> Unit,
    createStatus: String?,
    onCreateVariant: (RecipeVariantDto) -> Unit
) {
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
                text = "Connexion requise pour creer une variante",
                containerColor = WarningBg,
                contentColor = WarningText
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                val hasBlankIngredient = ingredients.any { it.name.trim().isEmpty() }
                val hasBlankStep = steps.any { it.step.trim().isEmpty() }
                val hasNegativeDuration = steps.any { it.duration < 0 }
                val remainingIngredients = ingredients.count { !it.deleted && it.name.trim().isNotEmpty() }
                val remainingSteps = steps.count { !it.deleted && it.step.trim().isNotEmpty() }

                val error = when {
                    name.trim().isEmpty() -> "Le titre de la variante est requis"
                    description.trim().isEmpty() -> "La description est requise"
                    ingredients.isEmpty() -> "Ajoutez au moins un ingredient"
                    steps.isEmpty() -> "Ajoutez au moins une etape"
                    hasBlankIngredient -> "Chaque ingredient doit avoir un nom"
                    hasBlankStep -> "Chaque etape doit avoir une description"
                    hasNegativeDuration -> "La duree des etapes doit etre positive"
                    remainingIngredients == 0 -> "Gardez au moins un ingredient non supprime"
                    remainingSteps == 0 -> "Gardez au moins une etape non supprimee"
                    else -> null
                }
                onLocalErrorChange(error)
                if (error == null && DataService.isLoggedIn) {
                    val request = RecipeVariantDto(
                        originalRecipeId = sourceRecipeId,
                        name = name.trim(),
                        description = description.trim(),
                        steps = steps.map {
                            RecipeVariantStepDto(
                                step = it.step.trim(),
                                duration = it.duration,
                                deleted = it.deleted,
                                original = it.original
                            )
                        },
                        ingredients = ingredients.map {
                            RecipeVariantIngredientDto(
                                name = it.name.trim(),
                                quantity = it.quantity.trim(),
                                deleted = it.deleted,
                                original = it.original
                            )
                        }
                    )
                    onCreateVariant(request)
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
                    text = "Creer la variante",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (localError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            MessageBanner(
                text = localError,
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
        if (createStatus == "OK") {
            Spacer(modifier = Modifier.height(10.dp))
            MessageBanner(
                text = "Variante creee avec succes",
                containerColor = SuccessBg,
                contentColor = SuccessText
            )
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
fun CreateVariantScreenPreview() {
    CreateVariantScreen(navController = rememberNavController(), recipeId = 1L)
}
