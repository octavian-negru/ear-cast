package app.earcast.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrimaryDestination.entries.forEach { destination ->
            navigationItem(
                destination = destination,
                selected = selected == destination,
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun navigationItem(
    destination: PrimaryDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when (destination) {
            PrimaryDestination.HOME -> R.string.nav_home
            PrimaryDestination.ASSIST -> R.string.nav_listen
            PrimaryDestination.PROFILE -> R.string.nav_profile_label
            PrimaryDestination.SETTINGS -> R.string.nav_settings
        }
    val icon =
        when (destination) {
            PrimaryDestination.HOME -> Icons.Filled.Home
            PrimaryDestination.ASSIST -> Icons.Filled.Headphones
            PrimaryDestination.PROFILE -> Icons.Filled.GraphicEq
            PrimaryDestination.SETTINGS -> Icons.Filled.Settings
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab,
                ).semantics {
                    this.selected = selected
                    this.role = Role.Tab
                }.heightIn(min = 64.dp)
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ).padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
