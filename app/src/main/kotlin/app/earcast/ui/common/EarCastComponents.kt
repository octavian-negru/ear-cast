@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    detail: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(detail, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                Text(action, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
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
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge)
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

/** Decorative waveform, drawn locally with no image downloads. */
@Composable
fun SoundMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(68.dp)) {
        val heights = listOf(0.2f, 0.4f, 0.7f, 1f, 0.65f, 0.35f, 0.6f, 0.85f, 0.5f, 0.25f, 0.4f)
        val spacing = 16.dp.toPx()
        val start = (size.width - spacing * (heights.size - 1)) / 2
        heights.forEachIndexed { index, fraction ->
            val half = size.height * fraction * 0.42f
            val x = start + index * spacing
            drawLine(color, Offset(x, center.y - half), Offset(x, center.y + half), 7.dp.toPx(), StrokeCap.Round)
        }
    }
}
