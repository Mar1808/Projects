package fr.univ_eiffel.ruge_frontend.ui.recipe

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled   .FavoriteBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fr.univ_eiffel.ruge_frontend.services.DataService
import fr.univ_eiffel.ruge_frontend.viewModel.PromotionViewModel
import fr.univ_eiffel.ruge_frontend.viewModel.RecipeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val Primary      = Color(0xFFE64A19)
private val PrimaryDark  = Color(0xFFC73D12)
private val PrimaryLight = Color(0xFFFFF2EE)
private val BgColor      = Color(0xFFFAF8F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val BorderColor  = Color(0xFFE8E0D5)
private val TextMain     = Color(0xFF1E1E1E)
private val TextMuted    = Color(0xFF888888)
private val FollowColor  = Color(0xFF2563EB)
private val CreateColor  = Color(0xFF16A34A)
private val HeartColor   = Color(0xFFD63755)
private val SuccessBg    = Color(0xFFE6F4EC)
private val SuccessText  = Color(0xFF1F6539)
private val DangerBg     = Color(0xFFFDE8E6)
private val DangerText   = Color(0xFFB23A33)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    navController: NavController,
    recipeId: Long,
    viewModel: RecipeViewModel = viewModel()
    // SUPPRESSION DU PromotionViewModel ICI
) {
    val recipe         = viewModel.recipeDetail
    val isVariante     = recipe?.recipeSourceId != 0L
    val isLiked        = viewModel.isLiked
    val isFollowing    = viewModel.isFollowing
    val isSuperLiked   = viewModel.isSuperLiked
    val isOwnRecipe    = viewModel.isOwnRecipe
    val likeCount      = viewModel.likeCount
    val isLoading      = viewModel.isLoading
    val errorMessage   = viewModel.errorMessage
    val successMessage = viewModel.successMessage
    val isAuthenticated = DataService.isLoggedIn

    var showSuperLikeDialog by remember { mutableStateOf(false) }
    var reviewText by remember { mutableStateOf("") }

    LaunchedEffect(recipeId) {
        viewModel.loadRecipeById(recipeId)
        // SUPPRESSION DU CHARGEMENT DES PROMOS ICI
    }

    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    val finalSuperLikePrice = recipe?.superLikePrice ?: 0.0

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->

        if (showSuperLikeDialog && recipe != null) {
            SuperLikeDialog(
                price = finalSuperLikePrice,
                onConfirm = {
                    showSuperLikeDialog = false
                    viewModel.superLike(recipeId) // <-- Appel normal
                },
                onDismiss = { showSuperLikeDialog = false }
            )
        }

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Primary) }
            return@Scaffold
        }

        if (recipe == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Recette introuvable.", color = TextMuted)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            item { HeroImage(imageUrl = recipe.imageUrl, recipeName = recipe.name) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Variant badge
                    if (isVariante) {
                        Surface(
                            onClick = {
                                recipe.recipeSourceId.let {
                                    navController.navigate("Recipe/$it")
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CallSplit,
                                    null,
                                    tint = Color(0xFF6D28D9),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Variante",
                                    color = Color(0xFF6D28D9),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Recipe name
                    Text(
                        text = recipe.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Meta chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        MetaChip(Icons.Default.Schedule, "${recipe.totalDuration} min")
                        MetaChip(Icons.Default.Favorite, "$likeCount like${if (likeCount > 1) "s" else ""}")
                        MetaChip(Icons.Default.Person, recipe.creatorName)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
            }

            // ── Feedback banners ─────────────────────────────────────────
            if (successMessage != null || errorMessage != null) {
                item {
                    val isSuccess = successMessage != null
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess) SuccessBg else DangerBg,
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
                    ) {
                        Text(
                            text = successMessage ?: errorMessage ?: "",
                            color = if (isSuccess) SuccessText else DangerText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // ── Action bar ───────────────────────────────────────────────
            item {
                ActionBar(
                    isAuthenticated  = isAuthenticated,
                    isOwnRecipe      = isOwnRecipe,
                    isLiked          = isLiked,
                    isFollowing      = isFollowing,
                    isSuperLiked     = isSuperLiked,
                    superLikePrice   = finalSuperLikePrice, // <-- Utilisation du prix avec promotion
                    onLike           = { viewModel.toggleLike(recipeId) },
                    onFollow         = { viewModel.toggleFollow(recipeId) },
                    onSuperLike      = { showSuperLikeDialog = true },
                    onCreateVariant  = { navController.navigate("CreateVariant/$recipeId") },
                    modifier         = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            // ── Description ──────────────────────────────────────────────
            item {
                RecipeCard(
                    title = "Description",
                    icon = Icons.Default.Notes,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = recipe.description.ifBlank { "Aucune description renseignée." },
                        color = if (recipe.description.isBlank()) TextMuted else TextMain,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        fontStyle = if (recipe.description.isBlank()) FontStyle.Italic else FontStyle.Normal
                    )
                }
            }

            // ── Ingredients ──────────────────────────────────────────────
            item {
                RecipeCard(
                    title = "Ingrédients",
                    icon = Icons.Default.ShoppingBasket,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (recipe.ingredients.isEmpty()) {
                        EmptySection("Aucun ingrédient renseigné")
                    } else {
                        Column {
                            recipe.ingredients.forEach { ing ->
                                IngredientRow(
                                    ing.name,
                                    ing.quantity,
                                    isVariante = isVariante
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }


            item {
                RecipeCard(
                    title = "Étapes de préparation",
                    icon = Icons.Default.FormatListNumbered,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (recipe.steps.isEmpty()) {
                        EmptySection("Aucune étape renseignée")
                    } else {
                        Column {
                            recipe.steps.forEachIndexed { index, step ->
                                StepRow(
                                    index + 1,
                                    step.step,
                                    step.duration,
                                    isVariante = isVariante
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            item {
                RecipeCard(
                    title = "Avis & Commentaires",
                    icon = Icons.Default.Comment,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (recipe.reviews.isEmpty()) {
                        EmptySection("Aucun avis pour le moment.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recipe.reviews.forEach { review ->
                                ReviewItem(review)
                            }
                        }
                    }

                    // Review form (authenticated users only)
                    if (isAuthenticated) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Laisser un avis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Partagez votre expérience…", color = TextMuted) },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.postReview(recipeId, reviewText) {
                                    reviewText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Publier l'avis", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─── Super like dialog ────────────────────────────────────────────────────────

@Composable
private fun SuperLikeDialog(
    price: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceColor,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, null, tint = HeartColor, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Super Like", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildString {
                        append("Super liker cette recette coûte ")
                        if (price > 0) append("%.2f €".format(price)) else append("0 € (Offert !)")
                        append(".\nCette action est irréversible.")
                    },
                    color = TextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.5.dp)
                    ) { Text("Annuler") }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = HeartColor),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirmer${if (price > 0) " (%.2f €)".format(price) else " (Gratuit)"}")
                    }
                }
            }
        }
    }
}

// ─── Action bar ───────────────────────────────────────────────────────────────

@Composable
private fun ActionBar(
    isAuthenticated: Boolean,
    isOwnRecipe: Boolean,
    isLiked: Boolean,
    isFollowing: Boolean,
    isSuperLiked: Boolean,
    superLikePrice: Double,
    onLike: () -> Unit,
    onFollow: () -> Unit,
    onSuperLike: () -> Unit,
    onCreateVariant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isAuthenticated && !isOwnRecipe) {
            // Ligne 1 : Like + Follow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    modifier     = Modifier.weight(1f),
                    icon         = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                    label        = "J'aime",
                    filled       = isLiked,
                    fillColor    = Primary,
                    contentColor = Primary,
                    onClick      = onLike
                )
                ActionButton(
                    modifier     = Modifier.weight(1f),
                    icon         = if (isFollowing) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label        = if (isFollowing) "Suivi" else "Suivre",
                    filled       = isFollowing,
                    fillColor    = FollowColor,
                    contentColor = FollowColor,
                    onClick      = onFollow
                )
            }

            Spacer(Modifier.height(8.dp))

            // Ligne 2 : Super like (centré, largeur naturelle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionButton(
                    icon         = if (isSuperLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label        = if (isSuperLiked) "Super liké" else "Super like",
                    badge        = if (!isSuperLiked) (if (superLikePrice > 0) "%.2f €".format(superLikePrice) else "Gratuit") else null,
                    filled       = isSuperLiked,
                    fillColor    = HeartColor,
                    contentColor = HeartColor,
                    enabled      = !isSuperLiked,
                    onClick      = onSuperLike
                )
            }

            Spacer(Modifier.height(8.dp))
        }
        if (isAuthenticated) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionButton(
                    icon         = Icons.Default.CallSplit,
                    label        = "Creer une variante",
                    filled       = false,
                    fillColor    = CreateColor,
                    contentColor = CreateColor,
                    onClick      = onCreateVariant
                )
            }
        }
    }

}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    filled: Boolean,
    fillColor: Color,
    contentColor: Color,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (filled) fillColor else SurfaceColor
    val fg = if (filled) Color.White else contentColor

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        color = if (!enabled && filled) fillColor.copy(alpha = 0.7f) else bg,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.5.dp),
        shadowElevation = 1.dp,
        modifier = modifier.animateContentSize()   // ← modifier en premier
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)  // ← centré
        ) {
            Icon(icon, label, tint = fg, modifier = Modifier.size(17.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (filled) Color.White.copy(alpha = 0.2f) else Color(0xFFFFF0F3)
                ) {
                    Text(
                        badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (filled) Color.White else HeartColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}


// ─── Review item ──────────────────────────────────────────────────────────────

@Composable
private fun ReviewItem(review: fr.univ_eiffel.ruge_frontend.dto.RecipeReviewDto) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAF8F5),
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Avatar initial
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        review.authorName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text(review.authorName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextMain)
                    Text(review.dateReview, fontSize = 12.sp, color = TextMuted)
                }
            }
            Text(review.comment, fontSize = 14.sp, color = TextMain, lineHeight = 21.sp)
        }
    }
}

// ─── Native image loader ──────────────────────────────────────────────────────

@Composable
private fun NativeNetworkImage(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val bmp = BitmapFactory.decodeStream(connection.inputStream)
                    bitmap = bmp?.asImageBitmap()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    if (bitmap != null) {
        Image(bitmap!!, null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(Color(0xFFEDE8E1)))
    }
}

// ─── Hero image ───────────────────────────────────────────────────────────────

@Composable
private fun HeroImage(imageUrl: String?, recipeName: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(Color(0xFFEDE8E1)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            NativeNetworkImage(imageUrl, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFFF5F0E8), Color(0xFFFAF7F2)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, null, tint = Color(0xFFC8BFB0), modifier = Modifier.size(72.dp))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgColor)))
        )
    }
}

// ─── Meta chip ────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = SurfaceColor,
        shadowElevation = 1.dp,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(13.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
        }
    }
}

// ─── Section card ─────────────────────────────────────────────────────────────

@Composable
private fun RecipeCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x12000000)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceColor,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Primary, modifier = Modifier.size(14.dp))
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            }
            content()
        }
    }
}

// ─── Ingredient row ───────────────────────────────────────────────────────────

@Composable
private fun IngredientRow(name: String, quantity: String, modifier: Modifier = Modifier, isVariante: Boolean) {
    val cleanName = name.replace("__DELETED__", "").replace("__KEEPED__", "")
    val isDeleted = isVariante && name.contains("__DELETED__")
    val isNew     = isVariante && !name.contains("__DELETED__") && !name.contains("__KEEPED__")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = when { isDeleted -> Color(0xFFFFF0F0); isNew -> Color(0xFFF0FDF4); else -> Color(0xFFFAF8F5) },
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (isDeleted) TextMuted else Primary))
            Text(cleanName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                color = if (isDeleted) TextMuted else TextMain, modifier = Modifier.weight(1f))
            Text(quantity, fontSize = 14.sp, color = TextMuted)
            if (isDeleted) DiffTag("supprimé", false)
            else if (isNew) DiffTag("nouveau", true)
        }
    }
}

// ─── Step row ─────────────────────────────────────────────────────────────────

@Composable
private fun StepRow(index: Int, stepText: String, duration: Int, modifier: Modifier = Modifier, isVariante: Boolean) {
    val cleanStep = stepText.replace("__DELETED__", "").replace("__KEEPED__", "")
    val isDeleted = isVariante && stepText.contains("__DELETED__")
    val isNew     = isVariante && !stepText.contains("__DELETED__") && !stepText.contains("__KEEPED__")

    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isDeleted) TextMuted else Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(cleanStep, fontSize = 15.sp, color = if (isDeleted) TextMuted else TextMain, lineHeight = 23.sp)
            if (isNew || isDeleted) {
                Spacer(Modifier.height(4.dp))
                if (isNew)     DiffTag("nouveau", true)
                if (isDeleted) DiffTag("supprimée", false)
            }
            if (duration > 0) {
                if (isNew || isDeleted) Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF5F0E8)) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        Text("$duration min", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

    }
}


@Composable
private fun DiffTag(label: String, isAdd: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isAdd) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    ) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAdd) Color(0xFF15803D) else Color(0xFFB91C1C),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            letterSpacing = 0.5.sp
        )
    }
}


@Composable
private fun EmptySection(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F6F2))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecipeScreenPreview() {
    RecipeScreen(navController = rememberNavController(), recipeId = 1L)
}