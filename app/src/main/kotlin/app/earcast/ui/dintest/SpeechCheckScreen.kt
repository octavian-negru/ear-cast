@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.dintest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.audiogram.SpeechProtocol
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionPage
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.ScreenHeader
import app.earcast.ui.common.SectionHeader
import app.earcast.ui.common.StatusTag
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone

/**
 * Listening-in-noise check: spoken digits in adaptive background noise. The
 * SNR-relative design makes the result meaningful on uncalibrated hardware —
 * the one screening where "just set a comfortable volume" is scientifically
 * fine. Mirrors the pure-tone check's phase structure and safety controls.
 */
@Composable
fun SpeechCheckScreen(
    onBack: () -> Unit,
    viewModel: SpeechCheckStateModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose { viewModel.mute() }
    }

    ActionPage(showActions = state.phase == SpeechCheckPhase.IN_PROGRESS, actions = {
        ActionButton(
            stringResource(R.string.check_mute),
            viewModel::mute,
            Modifier.fillMaxWidth(),
            style = ActionStyle.DANGER,
        )
    }) {
        ScreenHeader(title = stringResource(R.string.din_title), onBack = onBack)
        NoticeCard()

        when (state.phase) {
            SpeechCheckPhase.NOT_STARTED -> NotStarted(onStart = viewModel::start)
            SpeechCheckPhase.IN_PROGRESS -> InProgress(state = state, viewModel = viewModel)
            SpeechCheckPhase.DONE -> Results(state = state)
        }
    }
}

@Composable
private fun NoticeCard() {
    CollapsibleNotice(
        title = stringResource(R.string.estimate_summary),
        body = stringResource(R.string.din_notice),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

@Composable
private fun NotStarted(onStart: () -> Unit) {
    SurfaceCard(tone = SurfaceTone.TINT, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.din_prepare_title), index = "01")
        Text(
            stringResource(R.string.din_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
        )
        ActionButton(
            label = stringResource(R.string.din_start),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InProgress(
    state: SpeechCheckState,
    viewModel: SpeechCheckStateModel,
) {
    Column {
        SurfaceCard(tone = SurfaceTone.TINT, modifier = Modifier.fillMaxWidth()) {
            StatusTag(
                stringResource(R.string.din_progress, state.tripletNumber, state.totalTriplets),
                active = state.isPlaying,
            )
            LinearProgressIndicator(
                progress = { state.tripletNumber.toFloat() / state.totalTriplets },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Text(
                if (state.isPlaying) {
                    stringResource(R.string.din_playing)
                } else {
                    stringResource(R.string.din_enter_prompt)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
            EnteredDigits(state.entered)
        }
        Keypad(enabled = !state.isPlaying, onDigit = viewModel::tapDigit)

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                label = stringResource(R.string.din_backspace),
                onClick = viewModel::backspace,
                enabled = state.entered.isNotEmpty(),
                modifier = Modifier.weight(1f),
                style = ActionStyle.SECONDARY,
            )
            ActionButton(
                label = stringResource(R.string.din_submit),
                onClick = viewModel::submit,
                enabled = state.entered.size == SpeechProtocol.TRIPLET_SIZE && !state.isPlaying,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EnteredDigits(entered: List<Int>) {
    val display =
        List(SpeechProtocol.TRIPLET_SIZE) { index ->
            entered.getOrNull(index)?.toString() ?: "–"
        }.joinToString("  ")
    Text(
        display,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(vertical = 14.dp),
    )
}

@Composable
private fun Keypad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(0))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    ActionButton(
                        label = digit.toString(),
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        style = ActionStyle.SECONDARY,
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(state: SpeechCheckState) {
    SurfaceCard(tone = SurfaceTone.TINT, modifier = Modifier.fillMaxWidth()) {
        StatusTag(stringResource(R.string.din_complete), active = true)
        if (state.pinnedAtEdge) {
            Text(
                stringResource(R.string.din_result_pinned),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Text(
                stringResource(R.string.din_result_value, state.srtSnrDb),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                stringResource(
                    when (state.band) {
                        SpeechCheckBand.STRONG -> R.string.din_band_strong
                        SpeechCheckBand.MID -> R.string.din_band_mid
                        SpeechCheckBand.WEAKER -> R.string.din_band_weaker
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            stringResource(R.string.check_results_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
