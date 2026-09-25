@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SurfaceTone {
    PLAIN,
    TINT,
    WARM,
    DARK,
    DANGER,
}

enum class ActionStyle {
    PRIMARY,
    SECONDARY,
    DANGER,
}

@Composable
fun EarPage(
    verticalSpacing: Dp = 12.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    tone: SurfaceTone = SurfaceTone.PLAIN,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background =
        when (tone) {
            SurfaceTone.PLAIN -> colors.surface
            SurfaceTone.TINT -> colors.primaryContainer
            SurfaceTone.WARM -> colors.tertiaryContainer
            SurfaceTone.DARK -> colors.onBackground
            SurfaceTone.DANGER -> colors.errorContainer
        }
    val border =
        when (tone) {
            SurfaceTone.DARK -> Color.Transparent
            SurfaceTone.DANGER -> colors.error.copy(alpha = 0.28f)
            else -> colors.outlineVariant.copy(alpha = 0.6f)
        }
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(background)
                .border(BorderStroke(1.dp, border), MaterialTheme.shapes.large)
                .padding(padding),
        content = content,
    )
}

@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ActionStyle = ActionStyle.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val background =
        when (style) {
            ActionStyle.PRIMARY -> colors.primary
            ActionStyle.SECONDARY -> colors.surfaceVariant
            ActionStyle.DANGER -> colors.error
        }
    val foreground =
        when (style) {
            ActionStyle.PRIMARY -> colors.onPrimary
            ActionStyle.SECONDARY -> colors.onSurface
            ActionStyle.DANGER -> colors.onError
        }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Icon(icon ?: Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    index: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (index != null) {
            Text(index, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun StatusTag(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val containerColor =
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(containerColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun InlineChoices(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(containerColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (!enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                } else if (selected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

/** Keep a section heading attached to its controls, with larger gaps between sections. */
@Composable
fun SectionGroup(
    title: String,
    index: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, index = index)
        content()
    }
}
