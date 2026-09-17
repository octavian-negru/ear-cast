@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.hearingtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.common.AudioEar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.ScreenHeader
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPage
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSectionLabel
import app.earcast.ui.common.StudioStatus
import app.earcast.ui.common.StudioTone
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

    StudioPage {
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
    StudioPanel(tone = StudioTone.TINT, modifier = Modifier.fillMaxWidth()) {
        StudioSectionLabel(stringResource(R.string.check_prepare_title), index = "01")
        Text(
            stringResource(R.string.check_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
        )
        BigButton(stringResource(R.string.check_start), onClick = onStart)
        StudioButton(
            label = stringResource(R.string.check_manual_entry),
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = StudioButtonStyle.SECONDARY,
        )
    }
}

@Composable
private fun InProgress(
    state: ToneCheckState,
    viewModel: ToneCheckStateModel,
) {
    val questionText =
        if (state.isPlaying) stringResource(R.string.check_playing) else stringResource(R.string.check_question)
    Column {
        StudioPanel(tone = StudioTone.DARK, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StudioStatus(
                    stringResource(R.string.check_progress, state.completed + 1, state.total),
                    active = state.isPlaying,
                )
                Text(
                    stringResource(
                        R.string.check_ear_freq,
                        earLabel(state.currentEar),
                        state.currentFrequencyHz?.toInt() ?: 0,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.background,
                )
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
            Text(
                questionText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        }

        BigButton(stringResource(R.string.check_heard), onClick = viewModel::onHeard)
        StudioButton(
            label = stringResource(R.string.check_not_heard),
            onClick = viewModel::onNotHeard,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = StudioButtonStyle.SECONDARY,
        )
        StudioButton(
            label = stringResource(R.string.check_replay),
            onClick = viewModel::replay,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = StudioButtonStyle.SECONDARY,
        )

        SafetyControls(state = state, viewModel = viewModel)
    }
}

@Composable
private fun SafetyControls(
    state: ToneCheckState,
    viewModel: ToneCheckStateModel,
) {
    val sliderDescription = stringResource(R.string.check_volume_cap_slider)
    StudioPanel(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), tone = StudioTone.WARM) {
        Text(stringResource(R.string.check_volume_cap), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = state.masterCap,
            onValueChange = viewModel::setMasterCap,
            modifier = Modifier.semantics { contentDescription = sliderDescription },
        )
        StudioButton(
            label = stringResource(R.string.check_mute),
            onClick = viewModel::mute,
            modifier = Modifier.fillMaxWidth(),
            style = StudioButtonStyle.DANGER,
        )
    }
}

@Composable
private fun Results(
    state: ToneCheckState,
    onRestart: () -> Unit,
) {
    var showShare by rememberSaveable { mutableStateOf(false) }
    Column {
        StudioStatus(stringResource(R.string.check_complete), active = true)
        state.audiogram?.let { ResultsChart(it) }
        DetailSection(title = stringResource(R.string.details_section)) {
            state.audiogram?.let { AudiogramTable(it) }
            state.gains.forEach { GainTable(it) }
        }
        SoundPreviewCard()
        CollapsibleNotice(
            title = stringResource(R.string.estimate_summary),
            body = stringResource(R.string.check_results_disclaimer),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        BigButton(stringResource(R.string.check_run_again), onClick = onRestart)
        state.audiogram?.let { audiogram ->
            StudioButton(
                label = stringResource(R.string.check_share),
                onClick = { showShare = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = StudioButtonStyle.SECONDARY,
            )
            if (showShare) {
                ProfileShareDialog(audiogram = audiogram, onDismiss = { showShare = false })
            }
        }
    }
}

@Composable
private fun ResultsChart(audiogram: HearingCurve) {
    StudioPanel(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(stringResource(R.string.check_chart_title), style = MaterialTheme.typography.titleSmall)
        ProfileChart(audiogram, modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.check_chart_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AudiogramTable(audiogram: HearingCurve) {
    StudioPanel(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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

@Composable
private fun GainTable(summary: ChannelGainSummary) {
    StudioPanel(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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

@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
) {
    StudioButton(
        label = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun earLabel(ear: AudioEar?): String =
    when (ear) {
        AudioEar.LEFT -> stringResource(R.string.ear_left)
        AudioEar.RIGHT -> stringResource(R.string.ear_right)
        null -> "—"
    }
