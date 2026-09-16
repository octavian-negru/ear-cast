@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.manualentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.audiogram.HearingPoint
import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import app.earcast.ui.hearingtest.ProfileChart
import kotlin.math.roundToInt

private data class TonePreviewData(
    val ear: AudioEar,
    val selectedFrequency: Double,
    val selectedLevel: Int,
    val previewState: TonePreviewState,
)

/** UI-only editing of the same per-ear thresholds used by manual profile saving. */
@Composable
internal fun ProfilePlotEditor(
    levels: Map<AudioEar, Map<Double, Int>>,
    frequencies: List<Double>,
    previewState: TonePreviewState,
    onChange: (AudioEar, Double, Int) -> Unit,
    onPreview: (AudioEar, Double) -> Unit,
    onStopPreview: () -> Unit,
) {
    var selectedEar by rememberSaveable { mutableStateOf(AudioEar.RIGHT) }
    var selectedFrequency by rememberSaveable { mutableStateOf(frequencies.firstOrNull() ?: 1000.0) }
    val audiogram =
        remember(levels) {
            HearingCurve(
                levels.flatMap { (ear, entries) ->
                    entries.map { (frequency, level) ->
                        HearingPoint(ear, FrequencyHz(frequency), HearingDb(level.toDouble()))
                    }
                },
            )
        }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.manual_chart_title), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(AudioEar.RIGHT, AudioEar.LEFT).forEachIndexed { index, ear ->
                    SegmentedButton(
                        selected = selectedEar == ear,
                        onClick = { selectedEar = ear },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(earLabel(ear)) }
                }
            }
            AudiogramEntryControl(
                ear = selectedEar,
                frequency = selectedFrequency,
                levelDbHl = levels.getValue(selectedEar).getValue(selectedFrequency),
                frequencies = frequencies,
                onFrequencyChange = { selectedFrequency = it },
                onLevelChange = { level -> onChange(selectedEar, selectedFrequency, level) },
            )
            Text(stringResource(R.string.manual_chart_hint), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.manual_level_axis), style = MaterialTheme.typography.labelMedium)
            ProfileChart(
                audiogram = audiogram,
                description = stringResource(R.string.manual_chart_description, earLabel(selectedEar)),
                onPointChange = { frequency, level ->
                    selectedFrequency = frequency
                    val step = ProfileEditorModel.LEVEL_STEP_DB
                    onChange(selectedEar, frequency, (level / step).roundToInt() * step)
                },
            )
            Text(stringResource(R.string.manual_frequency_axis), style = MaterialTheme.typography.labelMedium)
            TonePreviewControl(
                preview =
                    TonePreviewData(
                        ear = selectedEar,
                        selectedFrequency = selectedFrequency,
                        selectedLevel = levels.getValue(selectedEar).getValue(selectedFrequency),
                        previewState = previewState,
                    ),
                onPreview = onPreview,
                onStopPreview = onStopPreview,
            )
        }
    }
}

@Composable
private fun AudiogramEntryControl(
    ear: AudioEar,
    frequency: Double,
    levelDbHl: Int,
    frequencies: List<Double>,
    onFrequencyChange: (Double) -> Unit,
    onLevelChange: (Int) -> Unit,
) {
    val levelDescription = stringResource(R.string.manual_entry_level_slider, earLabel(ear), frequency.toInt())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.manual_entry_selected, earLabel(ear), frequency.toInt()),
            style = MaterialTheme.typography.titleSmall,
        )
        FrequencyPicker(frequency, frequencies, onFrequencyChange)
        Text(
            stringResource(R.string.manual_entry_level, levelDbHl),
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = levelDbHl.toFloat(),
            onValueChange = { level ->
                val step = ProfileEditorModel.LEVEL_STEP_DB
                onLevelChange((level / step).roundToInt() * step)
            },
            valueRange =
                ProfileEditorModel.MIN_LEVEL_DB_HL.toFloat()..ProfileEditorModel.MAX_LEVEL_DB_HL.toFloat(),
            steps =
                (ProfileEditorModel.MAX_LEVEL_DB_HL - ProfileEditorModel.MIN_LEVEL_DB_HL) /
                    ProfileEditorModel.LEVEL_STEP_DB -
                    1,
            modifier = Modifier.semantics { contentDescription = levelDescription },
        )
    }
}

@Composable
private fun FrequencyPicker(
    selectedFrequency: Double,
    frequencies: List<Double>,
    onFrequencyChange: (Double) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manual_preview_frequency, selectedFrequency.toInt()))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            frequencies.forEach { frequency ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manual_preview_frequency, frequency.toInt())) },
                    onClick = {
                        onFrequencyChange(frequency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TonePreviewControl(
    preview: TonePreviewData,
    onPreview: (AudioEar, Double) -> Unit,
    onStopPreview: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
        Text(stringResource(R.string.manual_preview_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.manual_preview_description), style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(
                R.string.manual_preview_selected,
                earLabel(preview.ear),
                preview.selectedFrequency.toInt(),
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            stringResource(R.string.manual_preview_level, preview.selectedLevel),
            style = MaterialTheme.typography.labelMedium,
        )
        if (preview.previewState.isPlaying) {
            Button(onClick = onStopPreview, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manual_preview_stop))
            }
        } else {
            Button(
                onClick = { onPreview(preview.ear, preview.selectedFrequency) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.manual_preview_play))
            }
        }
        Text(
            stringResource(R.string.manual_preview_safety),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun earLabel(ear: AudioEar): String {
    val label = if (ear == AudioEar.RIGHT) R.string.chart_legend_right else R.string.chart_legend_left
    return stringResource(label)
}
