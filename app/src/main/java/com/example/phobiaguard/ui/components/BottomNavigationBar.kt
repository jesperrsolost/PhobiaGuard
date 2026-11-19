package com.example.phobiaguard.ui.components


import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.phobiaguard.ui.icons.CameraIcon
import com.example.phobiaguard.ui.icons.EyeIcon
import com.example.phobiaguard.ui.icons.PersonIcon
import com.example.phobiaguard.ui.navigation.CameraRo
import com.example.phobiaguard.ui.navigation.MainRo
import com.example.phobiaguard.ui.navigation.SettingsRoute

data class BottomNavItem(
    val route: Any,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(CameraRo, "Camera", CameraIcon),
    BottomNavItem(MainRo, "Monitoring", EyeIcon),
    BottomNavItem(SettingsRoute, "UserSettings", PersonIcon)
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (BottomNavItem) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route::class.qualifiedName,
                onClick = { onNavigate(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}