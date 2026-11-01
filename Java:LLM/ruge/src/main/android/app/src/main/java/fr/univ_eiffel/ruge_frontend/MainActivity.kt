package fr.univ_eiffel.ruge_frontend

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.dto.SimpleRecipeDto
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.services.NotificationService
import fr.univ_eiffel.ruge_frontend.ui.account.LoginScreen
import fr.univ_eiffel.ruge_frontend.ui.account.MyAccountScreen
import fr.univ_eiffel.ruge_frontend.ui.account.RegisterScreen
import fr.univ_eiffel.ruge_frontend.ui.account.WalletScreen
import fr.univ_eiffel.ruge_frontend.ui.admin.*
import fr.univ_eiffel.ruge_frontend.ui.layout.Footer
import fr.univ_eiffel.ruge_frontend.ui.layout.Header
import fr.univ_eiffel.ruge_frontend.ui.recipe.CreateRecipeScreen
import fr.univ_eiffel.ruge_frontend.ui.recipe.CreateVariantScreen
import fr.univ_eiffel.ruge_frontend.ui.recipe.RecipeScreen
import fr.univ_eiffel.ruge_frontend.viewModel.NotificationViewModel
import fr.univ_eiffel.ruge_frontend.viewModel.RecipeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val notificationService = remember { NotificationService(context) }
                    val notificationViewModel: NotificationViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return NotificationViewModel(notificationService) as T
                            }
                        }
                    )

                    LaunchedEffect(Unit) {
                        notificationViewModel.startChecking()
                    }

                    NavHost(navController = navController, startDestination = "Home") {
                        composable("Home") { HomeScreen(navController) }
                        composable("Register") { RegisterScreen(navController) }
                        composable("Login") { LoginScreen(navController) }
                        composable("Recipe/{id}") { backStackEntry ->
                            val recipeId = backStackEntry.arguments?.getString("id")
                            if (recipeId != null) RecipeScreen(navController, recipeId.toLong()) else HomeScreen(navController)
                        }
                        composable("MyAccount") { MyAccountScreen(navController) }
                        composable("CreateRecipe") { CreateRecipeScreen(navController) }
                        composable("CreateVariant/{id}") { backStackEntry ->
                            val recipeId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                            if (recipeId != null) CreateVariantScreen(navController, recipeId) else HomeScreen(navController)
                        }
                        composable("AdminDashboard") { AdminDashboardScreen(navController) }
                        composable("AdminManageUsers") { AdminManageUsersScreen(navController) }
                        composable("AdminManageRecipes") { AdminManageRecipesScreen(navController) }
                        composable("AdminFinances") { AdminFinancesScreen(navController) }
                        composable("AdminBanHistory/{userId}") { backStackEntry ->
                            val userIdStr = backStackEntry.arguments?.getString("userId")
                            if (userIdStr != null) AdminBanHistoryScreen(navController, userIdStr.toLong())
                        }
                        composable("AdminPromotions") { AdminPromotionsScreen(navController) }
                        composable("UserWallet") { WalletScreen(navController = navController) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: RecipeViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    val recipes = viewModel.recipes
    var page by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadRecipes(page)
        page += 1
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(modifier = Modifier, navController = navController)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5DC))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Rechercher une recette...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFFE44D26)
                ),
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchRecipes(searchQuery) }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Rechercher", tint = Color(0xFFE44D26))
                    }
                }
            )

        }
        if (DataService.isLoggedIn) {
            Button(
                onClick = { viewModel.searchRecipesAi(searchQuery) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8A2BE2),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Recherche IA",
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "IA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recipes) { recipe ->
                RecipeCard(recipe = recipe, onClick = { navController.navigate("Recipe/${recipe.id}") })
            }
            if(searchQuery == "") {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { viewModel.loadRecipes(page); page += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE44D26)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Charger plus", color = Color.White)
                        }
                    }
                }
            }

        }

        Footer(modifier = Modifier, navController = navController, "Home")
    }
}

@Composable
fun RecipeCard(recipe: SimpleRecipeDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            NativeNetworkImage(url = recipe.imageUrl, modifier = Modifier.fillMaxWidth().height(150.dp))
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = recipe.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = "Temps", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${recipe.totalDuration} min", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Par ${recipe.creatorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun NativeNetworkImage(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val androidBitmap = BitmapFactory.decodeStream(input)
                    bitmap = androidBitmap?.asImageBitmap()
                } catch (e: Exception) { println("Image download error: ${e.message}") }
            }
        }
    }

    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = "Image de la recette", modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(Color.LightGray))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}