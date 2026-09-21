@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The open rings are the EarCast identity, not a representation of hearing measurements. */
@Composable
fun SoundOrbit(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier) {
        val diameter = minOf(size.width, size.height)
        val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        repeat(5) { ring ->
            val inset = diameter * (0.07f + ring * 0.085f)
            val extent = diameter - inset * 2
            drawArc(
                color = color.copy(alpha = 1f - ring * 0.12f),
                startAngle = -65f + ring * 13f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = origin + Offset(inset, inset),
                size = Size(extent, extent),
                style = Stroke(width = diameter * 0.019f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun BrandBar(trailing: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SoundOrbit(Modifier.size(32.dp))
        Text("earcast", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FeatureRoute(
    number: String,
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun JourneyStep(
    number: String,
    title: String,
    detail: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Text(number, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Two working areas on tablets, one natural reading order on phones. */
@Composable
fun AdaptiveSplit(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 640.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(14.dp)) { primary() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { secondary() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                primary()
                secondary()
            }
        }
    }
}
