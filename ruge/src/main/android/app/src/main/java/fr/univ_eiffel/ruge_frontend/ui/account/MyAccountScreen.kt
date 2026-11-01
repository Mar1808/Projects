package fr.univ_eiffel.ruge_frontend.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.NativeNetworkImage
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.ui.layout.Footer
import fr.univ_eiffel.ruge_frontend.ui.layout.Header
import fr.univ_eiffel.ruge_frontend.viewModel.UserViewModel
import fr.univ_eiffel.ruge_frontend.viewModel.WalletViewModel

private val BgColor        = Color(0xFFFAF8F5)
private val SurfaceColor   = Color(0xFFFFFFFF)
private val PrimaryColor   = Color(0xFFE64A19)
private val PrimaryLight   = Color(0xFFFFF2EE)
private val PrimaryDark    = Color(0xFFC73D12)
private val BorderColor    = Color(0xFFE8E0D5)
private val TextMain       = Color(0xFF1E1E1E)
private val TextMuted      = Color(0xFF888888)
private val SearchBg       = Color(0xFFE8D8B7)
private val DangerColor    = Color(0xFFB23A33)
private val DangerBg       = Color(0xFFFDE8E6)
private val CardBg         = Color(0xFFFAF8F5)

@Composable
fun MyAccountScreen(
    navController: NavController,
    viewModel: UserViewModel = viewModel(),
    walletViewModel: WalletViewModel = viewModel(),

) {
    val recipes = viewModel.myRecipes

    val walletInfo = walletViewModel.walletInfo
    val balance = walletInfo?.walletBalance ?: 0.0
    val formattedBalance = String.format(java.util.Locale.FRANCE, "%.2f", balance)

    LaunchedEffect(Unit) {
        viewModel.loadMyRecipesAndStats()
        walletViewModel.loadWallet()
    }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            Footer(
                modifier = Modifier,
                navController = navController,
                currentRoute = "MyAccount"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            item {
                Header(modifier = Modifier, navController)
            }


            item {
                ProfileHeroCard(
                    navController = navController,
                    recipeCount = recipes.size,
                    likecount = viewModel.likeCount,
                    reviewCount = viewModel.reviewCount,
                    walletBalance = formattedBalance
                )
            }


            item {
                WalletBanner(onClick = { navController.navigate("UserWallet") })
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }


            item {
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Mes recettes publiées"
                )
            }


            item {
                CreateRecipeButton(onClick = { navController.navigate("CreateRecipe") })
            }

            // Liste recettes
            if (recipes.isEmpty()) {
                item { EmptyState(message = "Aucune recette publiée pour le moment.") }
            } else {
                items(recipes) { recipe ->
                    RecipeCard(
                        name        = recipe.name,
                        duration    = recipe.totalDuration,
                        creatorName = recipe.creatorName,
                        imageUrl    = recipe.imageUrl,
                        onClick     = { navController.navigate("Recipe/${recipe.id}") }
                    )
                }
            }

            if (DataService.role == "ADMIN") {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { AdminBanner(onClick = { navController.navigate("AdminDashboard") }) }
            }

            item {
                LogoutButton(
                    onClick = {
                        DataService.authToken = ""
                        DataService.isLoggedIn = false
                        DataService.role = ""
                        DataService.userId = null
                        DataService.userEmail = ""
                        DataService.userName = ""
                        DataService.userProfilePictureUrl = ""
                        navController.navigate("Login") { popUpTo(0) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    navController: NavController,
    recipeCount: Int,
    likecount: Int,
    reviewCount: Int,
    walletBalance: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SearchBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mon Profil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            value = recipeCount.toString(),
                            label = if (recipeCount > 1) "recettes" else "recette"
                        )

                        StatChip(
                            icon = Icons.Default.ThumbUp,
                            value = likecount.toString(),
                            label = "likes"
                        )

                        StatChip(
                            icon = Icons.AutoMirrored.Filled.Comment,
                            value = reviewCount.toString(),
                            label = "avis"
                        )

                        StatChip(
                            icon = Icons.Default.AccountBalanceWallet,
                            value = "$walletBalance €",
                            label = "solde"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "$value $label",
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

// Bannière Portefeuille
@Composable
private fun WalletBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mon Portefeuille",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "Gérer mon solde et historique",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
    }
}

//Bouton créer une recette
@Composable
private fun CreateRecipeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Créer une recette",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun RecipeCard(
    name: String,
    duration: Int,
    creatorName: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                NativeNetworkImage(
                    url = imageUrl,
                    modifier = Modifier.fillMaxSize()
                )
                // si pas d'image
                if (imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF5F0E8), Color(0xFFFAF7F2))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFFB8AE9F),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Infos ────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Durée
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(text = "$duration min", fontSize = 12.sp, color = TextMuted)
                    }
                    // Auteur
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = creatorName,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Chevron ──────────────────────────────────────
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BorderColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
// Empty State
// ═══════════════════════════════════════════════════════════
@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F6F2))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = Color(0xFFD4C9BB),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 15.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}


// Banniere Admin

@Composable
private fun AdminBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DangerBg),
        border = BorderStroke(1.dp, Color(0xFFF7B9B4))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DangerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Espace Administration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerColor
                )
                Text(
                    text = "Gérer les contenus et utilisateurs",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DangerColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// Bouton Déconnexion

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(50.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerColor),
        border = BorderStroke(2.dp, DangerColor)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Se déconnecter",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun AccountPreview() {
    MyAccountScreen(navController = rememberNavController())
}