@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.SoundOrbit

enum class PrimaryDestination { HOME, PROFILE, ASSIST, SETTINGS }

@Composable
fun navigationBarContent(
    selected: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .selectableGroup()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PrimaryDestination.entries.forEach { destination ->
                DockItem(
                    destination,
                    selected == destination,
                    { onDestinationSelected(destination) },
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun primaryNavigationRail(
    selected: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
) {
    Column(
        Modifier
            .width(112.dp)
            .fillMaxHeight()
            .padding(12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.onBackground)
            .selectableGroup()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoundOrbit(Modifier.padding(top = 16.dp).size(44.dp), color = MaterialTheme.colorScheme.background)
        Spacer(Modifier.weight(1f))
        PrimaryDestination.entries.forEach { destination ->
            DockItem(
                destination,
                selected == destination,
                { onDestinationSelected(destination) },
                Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun DockItem(
    destination: PrimaryDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val foreground = if (selected) colors.onPrimaryContainer else colors.background
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.primaryContainer else colors.onBackground)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 62.dp)
            .padding(horizontal = 3.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(destination.icon(), null, Modifier.size(21.dp), tint = foreground)
        Text(
            stringResource(destination.label()),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            textAlign = TextAlign.Center,
        )
    }
}

private fun PrimaryDestination.label(): Int =
    when (this) {
        PrimaryDestination.HOME -> R.string.nav_home
        PrimaryDestination.PROFILE -> R.string.nav_profile_label
        PrimaryDestination.ASSIST -> R.string.nav_listen
        PrimaryDestination.SETTINGS -> R.string.nav_settings
    }

private fun PrimaryDestination.icon(): ImageVector =
    when (this) {
        PrimaryDestination.HOME -> Icons.Filled.Home
        PrimaryDestination.PROFILE -> Icons.Filled.GraphicEq
        PrimaryDestination.ASSIST -> Icons.Filled.Headphones
        PrimaryDestination.SETTINGS -> Icons.Filled.Settings
    }
