@file:Suppress("ktlint:standard:function-naming")

package app.openhearing.ui.manualentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.openhearing.R
import app.openhearing.audiogram.Audiogram
import app.openhearing.audiogram.Threshold
import app.openhearing.common.DecibelsHl
import app.openhearing.common.Ear
import app.openhearing.common.Hertz
import app.openhearing.ui.hearingtest.AudiogramChart
import kotlin.math.roundToInt

/** UI-only editing of the same per-ear thresholds used by manual profile saving. */
@Composable
internal fun ManualAudiogramEditor(
    levels: Map<Ear, Map<Double, Int>>,
    onChange: (Ear, Double, Int) -> Unit,
) {
    var selectedEar by rememberSaveable { mutableStateOf(Ear.RIGHT) }
    val audiogram =
        remember(levels) {
            Audiogram(
                levels.flatMap { (ear, entries) ->
                    entries.map { (frequency, level) -> Threshold(ear, Hertz(frequency), DecibelsHl(level.toDouble())) }
                },
            )
        }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.manual_chart_title), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(Ear.RIGHT, Ear.LEFT).forEachIndexed { index, ear ->
                    SegmentedButton(
                        selected = selectedEar == ear,
                        onClick = { selectedEar = ear },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(earLabel(ear)) }
                }
            }
            Text(stringResource(R.string.manual_chart_hint), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.manual_level_axis), style = MaterialTheme.typography.labelMedium)
            AudiogramChart(
                audiogram = audiogram,
                description = stringResource(R.string.manual_chart_description, earLabel(selectedEar)),
                onPointChange = { frequency, level ->
                    val step = ManualEntryViewModel.LEVEL_STEP_DB
                    onChange(selectedEar, frequency, (level / step).roundToInt() * step)
                },
            )
            Text(stringResource(R.string.manual_frequency_axis), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun earLabel(ear: Ear): String {
    val label = if (ear == Ear.RIGHT) R.string.chart_legend_right else R.string.chart_legend_left
    return stringResource(label)
}
