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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.common.AudioEar
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionPage
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.JourneyStep
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.ScreenHeader
import app.earcast.ui.common.StatusTag
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone
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

    ActionPage(showActions = state.phase == ToneCheckPhase.IN_PROGRESS, actions = {
        ActionButton(
            stringResource(R.string.check_mute),
            viewModel::mute,
            Modifier.fillMaxWidth(),
            style = ActionStyle.DANGER,
        )
    }) {
        ScreenHeader(title = stringResource(R.string.check_title), onBack = onBack)

        when (state.phase) {
            ToneCheckPhase.NOT_STARTED -> NotStarted(onStart = viewModel::start, onManualEntry = onManualEntry)
            ToneCheckPhase.IN_PROGRESS ->
                InProgress(state = state, viewModel = viewModel)
            ToneCheckPhase.DONE -> Results(state = state, onRestart = viewModel::start, onDone = onBack)
        }
        CalibrationNotice()
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
    PageHeading(stringResource(R.string.identity_tone_title))
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        JourneyStep(
            "01",
            stringResource(R.string.identity_prepare_one),
            stringResource(R.string.identity_prepare_one_detail),
        )
        JourneyStep(
            "02",
            stringResource(R.string.identity_prepare_two),
            stringResource(R.string.identity_prepare_two_detail),
        )
        JourneyStep(
            "03",
            stringResource(R.string.identity_prepare_three),
            stringResource(R.string.identity_prepare_three_detail),
        )
    }
    ActionButton(stringResource(R.string.check_start), onStart, Modifier.fillMaxWidth())
    ActionButton(
        stringResource(R.string.check_manual_entry),
        onManualEntry,
        Modifier.fillMaxWidth(),
        style = ActionStyle.SECONDARY,
    )
}

@Composable
private fun InProgress(
    state: ToneCheckState,
    viewModel: ToneCheckStateModel,
) {
    val questionText =
        if (state.isPlaying) stringResource(R.string.check_playing) else stringResource(R.string.check_question)
    Column {
        SurfaceCard(tone = SurfaceTone.TINT, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusTag(
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
            Text(
                questionText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        }

        BigButton(stringResource(R.string.check_heard), onClick = viewModel::onHeard)
        ActionButton(
            label = stringResource(R.string.check_not_heard),
            onClick = viewModel::onNotHeard,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = ActionStyle.SECONDARY,
        )
        ActionButton(
            label = stringResource(R.string.check_replay),
            onClick = viewModel::replay,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = ActionStyle.SECONDARY,
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
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), tone = SurfaceTone.WARM) {
        Text(stringResource(R.string.check_volume_cap), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = state.masterCap,
            onValueChange = viewModel::setMasterCap,
            modifier = Modifier.semantics { contentDescription = sliderDescription },
        )
    }
}

@Composable
private fun Results(
    state: ToneCheckState,
    onRestart: () -> Unit,
    onDone: () -> Unit,
) {
    var showShare by rememberSaveable { mutableStateOf(false) }
    Column {
        StatusTag(stringResource(R.string.check_complete), active = true)
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
        BigButton(stringResource(R.string.check_done_action), onClick = onDone)
        ActionButton(
            label = stringResource(R.string.check_run_again),
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = ActionStyle.SECONDARY,
        )
        state.audiogram?.let { audiogram ->
            ActionButton(
                label = stringResource(R.string.check_share),
                onClick = { showShare = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = ActionStyle.SECONDARY,
            )
            if (showShare) {
                ProfileShareDialog(audiogram = audiogram, onDismiss = { showShare = false })
            }
        }
    }
}

@Composable
private fun ResultsChart(audiogram: HearingCurve) {
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
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
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
    ActionButton(
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
