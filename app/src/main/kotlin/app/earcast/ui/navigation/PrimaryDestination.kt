package app.earcast.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R

enum class PrimaryDestination {
    HOME,
    ASSIST,
    PROFILE,
    SETTINGS,
}

@Composable
fun navigationBarContent(
    selected: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 2.dp,
    ) {
        NavigationBarItem(
            selected = selected == PrimaryDestination.HOME,
            onClick = { onDestinationSelected(PrimaryDestination.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
        )
        NavigationBarItem(
            selected = selected == PrimaryDestination.ASSIST,
            onClick = { onDestinationSelected(PrimaryDestination.ASSIST) },
            icon = { Icon(Icons.Filled.Headphones, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_listen)) },
        )
        NavigationBarItem(
            selected = selected == PrimaryDestination.PROFILE,
            onClick = { onDestinationSelected(PrimaryDestination.PROFILE) },
            icon = { Icon(Icons.Filled.GraphicEq, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_profile_label)) },
        )
        NavigationBarItem(
            selected = selected == PrimaryDestination.SETTINGS,
            onClick = { onDestinationSelected(PrimaryDestination.SETTINGS) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
