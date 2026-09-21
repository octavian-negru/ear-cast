@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.demo

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.SurfaceCard

/**
 * "Hear the difference" A/B demo card: plays the sample sound with a live
 * raw-vs-your-profile switch. Hidden until a profile exists; steps aside while
 * assist is running (mic loop and demo playback must not fight over audio).
 */
@Composable
fun SoundPreviewCard(
    modifier: Modifier = Modifier,
    viewModel: PreviewStateModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.available) return

    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.demo_desc),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (state.assistRunning) {
            Text(
                stringResource(R.string.demo_assist_running),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Switch(
                    checked = state.processedActive,
                    onCheckedChange = viewModel::setProcessed,
                )
                Text(
                    stringResource(R.string.demo_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            ActionButton(
                label =
                    if (state.playing) {
                        stringResource(R.string.demo_stop)
                    } else {
                        stringResource(R.string.demo_play)
                    },
                onClick = viewModel::togglePlayback,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = if (state.playing) ActionStyle.DANGER else ActionStyle.PRIMARY,
            )
            Text(
                stringResource(R.string.demo_flat_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
