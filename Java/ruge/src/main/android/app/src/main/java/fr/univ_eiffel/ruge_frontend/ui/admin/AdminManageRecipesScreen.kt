package fr.univ_eiffel.ruge_frontend.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.univ_eiffel.ruge_frontend.dto.RecipeDetailDto
import fr.univ_eiffel.ruge_frontend.viewModel.AdminRecipesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageRecipesScreen(
    navController: NavController,
    viewModel: AdminRecipesViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration des recettes") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Rechercher une recette ou un auteur") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (viewModel.recipes.isEmpty()) {
                    val emptyMessage = if (viewModel.searchQuery.isNotEmpty()) {
                        "Aucun résultat pour '${viewModel.searchQuery}'."
                    } else {
                        "Aucune recette trouvée."
                    }
                    Text(text = emptyMessage, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.recipes) { recipe ->
                            AdminRecipeCard(
                                recipe = recipe,
                                onCardClick = {
                                    navController.navigate("Recipe/${recipe.id}")
                                },
                                onDeleteConfirm = {
                                    viewModel.deleteRecipe(recipe.id)
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
fun AdminRecipeCard(
    recipe: RecipeDetailDto,
    onCardClick: () -> Unit,
    onDeleteConfirm: () -> Unit
) {
    var isConfirming by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recipe.name, fontWeight = FontWeight.Bold)
                Text(text = "Auteur : ${recipe.creatorName}", style = MaterialTheme.typography.bodySmall)
            }

            if (isConfirming) {
                TextButton(onClick = { isConfirming = false }) {
                    Text("Annuler")
                }
                Button(
                    onClick = {
                        isConfirming = false
                        onDeleteConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            } else {
                IconButton(onClick = { isConfirming = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}