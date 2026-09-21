@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.ui.hearingtest.ProfileChart

/** The saved audiogram is the primary instrument, with real values and conventional ear markers. */
@Composable
fun AudiogramCard(
    audiogram: HearingCurve?,
    onEdit: () -> Unit,
    actionLabel: String = stringResource(R.string.audiogram_edit),
) {
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.identity_profile_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.identity_hearing_map), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "dB HL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (audiogram != null && audiogram.thresholds.isNotEmpty()) {
                ProfileChart(audiogram)
            } else {
                Text(stringResource(R.string.audiogram_empty_detail), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}
