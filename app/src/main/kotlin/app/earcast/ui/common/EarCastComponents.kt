@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.earcast.R

@Composable
fun PageHeading(
    title: String,
    subtitle: String? = null,
) {
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    if (subtitle != null) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    if (onBack == null) {
        PageHeading(title, subtitle)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
    if (subtitle != null) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    detail: String,
    action: String,
    onClick: () -> Unit,
    emphasis: ActionCardEmphasis = ActionCardEmphasis.PRIMARY,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (emphasis == ActionCardEmphasis.PRIMARY) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            if (emphasis == ActionCardEmphasis.PRIMARY) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    ActionCardButtonContent(action)
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    ActionCardButtonContent(action)
                }
            }
        }
    }
}

enum class ActionCardEmphasis {
    PRIMARY,
    SECONDARY,
}

@Composable
private fun ActionCardButtonContent(action: String) {
    Text(action, style = MaterialTheme.typography.labelLarge)
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.padding(start = 8.dp),
    )
}

/** Long explanations stay available without overwhelming the primary controls. */
@Composable
fun DetailSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val state = stringResource(if (expanded) R.string.section_expanded else R.string.section_collapsed)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics { stateDescription = state },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), content = content)
        }
    }
}

@Composable
fun QuietNotice(
    title: String,
    body: String,
) {
    DetailSection(title) {
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CollapsibleNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val state = stringResource(if (expanded) R.string.section_expanded else R.string.section_collapsed)
    val toggleDescription = stringResource(if (expanded) R.string.notice_collapse else R.string.notice_expand)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier =
                        Modifier.semantics {
                            contentDescription = toggleDescription
                            stateDescription = state
                        },
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Decorative waveform, drawn locally with no image downloads. */
@Composable
fun SoundMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.fillMaxWidth().height(42.dp)) {
        val centerY = center.y
        val dotRadius = 5.dp.toPx()
        drawCircle(markerColor, dotRadius, Offset(12.dp.toPx(), centerY))
        val points =
            listOf(
                Offset(24.dp.toPx(), centerY),
                Offset(34.dp.toPx(), centerY - 10.dp.toPx()),
                Offset(44.dp.toPx(), centerY + 7.dp.toPx()),
                Offset(54.dp.toPx(), centerY - 16.dp.toPx()),
                Offset(64.dp.toPx(), centerY + 4.dp.toPx()),
                Offset(74.dp.toPx(), centerY),
            )
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color, start, end, 3.dp.toPx(), StrokeCap.Round)
        }
    }
}
