@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.hearingtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.common.AudioEar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.ScreenHeader
import app.earcast.ui.demo.SoundPreviewCard
import app.earcast.ui.share.ProfileShareDialog

/**
 * Hearing-check screen: runs the pure-tone screening through the phone speaker or
 * any connected headset (no AirPods needed), then shows the result as an
 * audiogram-style chart plus detailed tables and the suggested amplification.
 */
@Composable
fun ToneCheckScreen(
    onBack: () -> Unit,
    onManualEntry: () -> Unit = {},
    viewModel: ToneCheckStateModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose { viewModel.mute() }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.check_title), onBack = onBack)
        CalibrationNotice()

        when (state.phase) {
            ToneCheckPhase.NOT_STARTED -> NotStarted(onStart = viewModel::start, onManualEntry = onManualEntry)
            ToneCheckPhase.IN_PROGRESS ->
                InProgress(state = state, viewModel = viewModel)
            ToneCheckPhase.DONE -> Results(state = state, onRestart = viewModel::start)
        }
    }
}

@Composable
private fun CalibrationNotice() {
    CollapsibleNotice(
        title = stringResource(R.string.estimate_summary),
        body = stringResource(R.string.check_notice),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

@Composable
private fun NotStarted(
    onStart: () -> Unit,
    onManualEntry: () -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.check_intro),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        BigButton(stringResource(R.string.check_start), onClick = onStart)
        Spacer(Modifier.padding(4.dp))
        OutlinedButton(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.check_manual_entry), style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun InProgress(
    state: ToneCheckState,
    viewModel: ToneCheckStateModel,
) {
    Column {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        Text(
            stringResource(R.string.check_progress, state.completed + 1, state.total),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            stringResource(
                R.string.check_ear_freq,
                earLabel(state.currentEar),
                state.currentFrequencyHz?.toInt() ?: 0,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            if (state.isPlaying) {
                stringResource(R.string.check_playing)
            } else {
                stringResource(R.string.check_question)
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        BigButton(stringResource(R.string.check_heard), onClick = viewModel::onHeard)
        Spacer(Modifier.padding(4.dp))
        BigButton(stringResource(R.string.check_not_heard), onClick = viewModel::onNotHeard)
        Spacer(Modifier.padding(4.dp))
        OutlinedButton(
            onClick = viewModel::replay,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.check_replay), style = MaterialTheme.typography.labelLarge) }

        SafetyControls(state = state, viewModel = viewModel)
    }
}

@Composable
private fun SafetyControls(
    state: ToneCheckState,
    viewModel: ToneCheckStateModel,
) {
    val sliderDescription = stringResource(R.string.check_volume_cap_slider)
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.check_volume_cap), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = state.masterCap,
                onValueChange = viewModel::setMasterCap,
                modifier = Modifier.semantics { contentDescription = sliderDescription },
            )
            Button(
                onClick = viewModel::mute,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.check_mute), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun Results(
    state: ToneCheckState,
    onRestart: () -> Unit,
) {
    var showShare by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            stringResource(R.string.check_complete),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        state.audiogram?.let { ResultsChart(it) }
        state.audiogram?.let { AudiogramTable(it) }
        state.gains.forEach { GainTable(it) }
        SoundPreviewCard()
        CollapsibleNotice(
            title = stringResource(R.string.estimate_summary),
            body = stringResource(R.string.check_results_disclaimer),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        BigButton(stringResource(R.string.check_run_again), onClick = onRestart)
        Spacer(Modifier.padding(4.dp))
        state.audiogram?.let { audiogram ->
            OutlinedButton(
                onClick = { showShare = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.check_share), style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.padding(4.dp))
            if (showShare) {
                ProfileShareDialog(audiogram = audiogram, onDismiss = { showShare = false })
            }
        }
    }
}

@Composable
private fun ResultsChart(audiogram: HearingCurve) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.check_chart_title), style = MaterialTheme.typography.titleSmall)
            ProfileChart(audiogram, modifier = Modifier.padding(top = 12.dp))
            Text(
                stringResource(R.string.check_chart_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AudiogramTable(audiogram: HearingCurve) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.check_table_title), style = MaterialTheme.typography.titleSmall)
            listOf(AudioEar.RIGHT, AudioEar.LEFT).forEach { ear ->
                Text(
                    stringResource(R.string.ear_label, earLabel(ear)),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                audiogram.frequenciesFor(ear).forEach { f ->
                    val hl = audiogram.thresholdAt(ear, f)?.value?.toInt()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.hz_value, f.value.toInt()))
                        Text(stringResource(R.string.db_hl_value, hl?.toString() ?: "—"))
                    }
                }
            }
        }
    }
}

@Composable
private fun GainTable(summary: ChannelGainSummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.gain_title, earLabel(summary.ear)),
                style = MaterialTheme.typography.titleSmall,
            )
            summary.points.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.hz_value, p.frequency.value.toInt()))
                    Text(stringResource(R.string.gain_db_value, p.gainDb.toInt()))
                }
            }
        }
    }
}

@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun earLabel(ear: AudioEar?): String =
    when (ear) {
        AudioEar.LEFT -> stringResource(R.string.ear_left)
        AudioEar.RIGHT -> stringResource(R.string.ear_right)
        null -> "—"
    }
