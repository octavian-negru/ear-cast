@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.assist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.core.audio.StreamPhase
import app.earcast.core.audio.StreamStatus
import app.earcast.core.audio.speech.BassReduction
import app.earcast.core.audio.speech.EnhancementEngine
import app.earcast.core.audio.speech.EnhancementOptions
import app.earcast.core.audio.speech.InputMode
import app.earcast.core.audio.speech.NoiseStrength
import app.earcast.core.audio.speech.SpeechPresence
import app.earcast.core.audio.speech.VoiceBoost

@Composable
@Suppress("LongMethod")
internal fun SoundOptionsPanel(
    options: EnhancementOptions,
    enabled: Boolean,
    onChange: (EnhancementOptions) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.assist_sound_quality), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.assist_quality_description), style = MaterialTheme.typography.bodySmall)
            EngineChoice(options.speechEngine, enabled) { onChange(options.copy(speechEngine = it)) }
            Text(
                stringResource(
                    when (options.speechEngine) {
                        EnhancementEngine.RNNOISE -> R.string.assist_rnnoise_description
                        EnhancementEngine.DPDFNET -> R.string.assist_engine_description
                        EnhancementEngine.SPEEX -> R.string.assist_speex_description
                        EnhancementEngine.WIENER -> R.string.assist_wiener_description
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            QualityChoice(
                stringResource(R.string.assist_capture_mode),
                listOf(stringResource(R.string.assist_capture_natural), stringResource(R.string.assist_capture_call)),
                options.captureMode.ordinal,
                enabled,
            ) { onChange(options.copy(captureMode = InputMode.entries[it])) }
            val strengths =
                listOf(
                    stringResource(R.string.assist_quality_off),
                    stringResource(R.string.assist_quality_gentle),
                    stringResource(R.string.assist_quality_strong),
                )
            QualityChoice(
                stringResource(R.string.assist_speech_clarity),
                strengths,
                options.speechClarity.ordinal,
                enabled,
            ) {
                onChange(options.copy(speechClarity = SpeechPresence.entries[it]))
            }
            QualityChoice(
                stringResource(R.string.assist_voice_comfort),
                strengths,
                options.voiceComfort.ordinal,
                enabled,
            ) {
                onChange(options.copy(voiceComfort = BassReduction.entries[it]))
            }
            QualityChoice(
                stringResource(R.string.assist_noise_reduction),
                strengths,
                options.noiseReduction.ordinal,
                enabled,
            ) {
                onChange(options.copy(noiseReduction = NoiseStrength.entries[it]))
            }
            Text(stringResource(R.string.assist_noise_description), style = MaterialTheme.typography.bodySmall)
            QualityChoice(
                stringResource(R.string.assist_quiet_speech),
                strengths,
                options.quietSpeech.ordinal,
                enabled =
                    enabled &&
                        options.noiseReduction != NoiseStrength.OFF &&
                        options.speechEngine == EnhancementEngine.RNNOISE,
            ) {
                onChange(options.copy(quietSpeech = VoiceBoost.entries[it]))
            }
            Text(
                stringResource(
                    if (options.speechEngine == EnhancementEngine.RNNOISE) {
                        R.string.assist_quiet_speech_description
                    } else {
                        R.string.assist_other_engine_level_description
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!enabled) {
                Text(stringResource(R.string.assist_quality_restart), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EngineChoice(
    selected: EnhancementEngine,
    enabled: Boolean,
    onChange: (EnhancementEngine) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labels =
        mapOf(
            EnhancementEngine.RNNOISE to "RNNoise",
            EnhancementEngine.DPDFNET to "DPDFNet8",
            EnhancementEngine.SPEEX to "SpeexDSP",
            EnhancementEngine.WIENER to stringResource(R.string.assist_wiener_name),
        )
    Text(
        stringResource(R.string.assist_speech_engine),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(labels.getValue(selected))
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            labels.forEach { (engine, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    onChange(engine)
                })
            }
        }
    }
}

@Composable
private fun QualityChoice(
    title: String,
    labels: List<String>,
    selected: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                enabled = enabled,
                onClick = { onChange(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) { Text(label) }
        }
    }
}

@Composable
internal fun SessionAudioStatus(status: StreamStatus) {
    when (status.state) {
        StreamPhase.FAILED ->
            Text(
                status.message ?: stringResource(R.string.assist_connection_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        StreamPhase.RUNNING -> {
            Text(status.message.orEmpty(), style = MaterialTheme.typography.bodySmall)
            if (status.bluetoothCallAudio) {
                Text(stringResource(R.string.assist_bluetooth_call_quality), style = MaterialTheme.typography.bodySmall)
            } else if (status.monoOutput) {
                Text(stringResource(R.string.assist_mono_output), style = MaterialTheme.typography.bodySmall)
            }
        }
        else -> Unit
    }
}
