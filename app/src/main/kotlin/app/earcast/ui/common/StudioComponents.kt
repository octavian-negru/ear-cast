@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StudioTone {
    PLAIN,
    TINT,
    WARM,
    DARK,
    DANGER,
}

enum class StudioButtonStyle {
    PRIMARY,
    SECONDARY,
    DANGER,
}

@Composable
fun StudioPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun StudioPanel(
    modifier: Modifier = Modifier,
    tone: StudioTone = StudioTone.PLAIN,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background =
        when (tone) {
            StudioTone.PLAIN -> colors.surface
            StudioTone.TINT -> colors.primaryContainer
            StudioTone.WARM -> colors.tertiaryContainer
            StudioTone.DARK -> colors.onBackground
            StudioTone.DANGER -> colors.errorContainer
        }
    val border =
        when (tone) {
            StudioTone.DARK -> Color.Transparent
            StudioTone.DANGER -> colors.error.copy(alpha = 0.28f)
            else -> colors.outlineVariant
        }
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(background)
                .border(BorderStroke(1.dp, border), MaterialTheme.shapes.medium)
                .padding(padding),
        content = content,
    )
}

@Composable
fun StudioButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StudioButtonStyle = StudioButtonStyle.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container =
        when (style) {
            StudioButtonStyle.PRIMARY -> colors.primary
            StudioButtonStyle.SECONDARY -> Color.Transparent
            StudioButtonStyle.DANGER -> colors.error
        }
    val contentColor =
        when (style) {
            StudioButtonStyle.PRIMARY -> colors.onPrimary
            StudioButtonStyle.SECONDARY -> colors.onSurface
            StudioButtonStyle.DANGER -> colors.onError
        }
    val borderColor = if (style == StudioButtonStyle.SECONDARY) colors.outline else Color.Transparent
    val alpha = if (enabled) 1f else 0.42f
    Row(
        modifier =
            modifier
                .heightIn(min = 52.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(container.copy(alpha = if (container == Color.Transparent) 0f else alpha))
                .border(1.dp, borderColor.copy(alpha = alpha), MaterialTheme.shapes.medium)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val iconTint = contentColor.copy(alpha = alpha)
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = iconTint,
            )
        }
        Text(
            label,
            color = contentColor.copy(alpha = alpha),
            style = MaterialTheme.typography.labelLarge,
            modifier = if (icon == null) Modifier else Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun StudioSectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    index: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (index != null) {
            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    index,
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun StudioStatus(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val containerColor =
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(containerColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
fun StudioCommand(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .selectable(selected = false, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun StudioSplitRow(
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
fun StudioChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
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
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
