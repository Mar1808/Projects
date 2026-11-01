package fr.univ_eiffel.ruge_frontend.ui.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.univ_eiffel.ruge_frontend.R
import fr.univ_eiffel.ruge_frontend.ui.theme.Ruge

@Composable
fun Footer(modifier : Modifier, navController: NavController, currentRoute: String = "") {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color = Color(0xFFF0F0F0)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FooterButton(
            iconId = R.drawable.muffin,
            description = "muffin",
            isSelected = currentRoute == "Home",
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("Home") }
        )

        FooterButton(
            iconId = R.drawable.login,
            description = "login",
            isSelected = currentRoute == "Login",
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("Login") }
        )

        FooterButton(
            iconId = R.drawable.useradd,
            description = "register",
            isSelected = currentRoute == "Register",
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("Register") }
        )
    }
}

@Composable
fun FooterButton(
    iconId: Int,
    description: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val backgroundColor = if (isSelected) Color(0xFFE44D26).copy(alpha = 0.2f) else Color.Transparent

        Box(
            modifier = Modifier
                .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = description,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}