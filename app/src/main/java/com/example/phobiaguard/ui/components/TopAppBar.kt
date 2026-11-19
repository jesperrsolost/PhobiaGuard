package com.example.phobiaguard.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.phobiaguard.R

/**
 * Displays a Material 3 top app bar with a title, navigation content, and optional actions.
 *
 * @param title text displayed in the app bar.
 * @param navigationIcon composable content displayed in the navigation slot.
 * @param actions composable content displayed in the actions slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(text = title)
        },
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Preview(showBackground = true)
@Composable
private fun TopAppBarPreview() {
    TopAppBar(
        title = "Preview",
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Go back"
                )
            }
        }
    )
}
