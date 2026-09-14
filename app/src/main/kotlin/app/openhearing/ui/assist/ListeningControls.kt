package app.openhearing.ui.assist

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.openhearing.R
import app.openhearing.core.audio.AudioSessionState
import app.openhearing.core.audio.AudioSessionStatus
import app.openhearing.core.audio.speech.CaptureMode
import app.openhearing.core.audio.speech.ListeningOptions
import app.openhearing.core.audio.speech.NoiseReduction
import app.openhearing.core.audio.speech.QuietSpeech
import app.openhearing.core.audio.speech.SpeechClarity
import app.openhearing.core.audio.speech.VoiceComfort

@Composable
internal fun ListeningControls(options: ListeningOptions, enabled: Boolean, onChange: (ListeningOptions) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.assist_sound_quality), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.assist_quality_description), style = MaterialTheme.typography.bodySmall)
            QualityChoice(
                stringResource(R.string.assist_capture_mode),
                listOf(stringResource(R.string.assist_capture_natural), stringResource(R.string.assist_capture_call)),
                options.captureMode.ordinal,
                enabled,
            ) { onChange(options.copy(captureMode = CaptureMode.entries[it])) }
            val strengths = listOf(
                stringResource(R.string.assist_quality_off),
                stringResource(R.string.assist_quality_gentle),
                stringResource(R.string.assist_quality_strong),
            )
            QualityChoice(
                stringResource(R.string.assist_speech_clarity), strengths, options.speechClarity.ordinal, enabled,
            ) {
                onChange(options.copy(speechClarity = SpeechClarity.entries[it]))
            }
            QualityChoice(
                stringResource(R.string.assist_voice_comfort), strengths, options.voiceComfort.ordinal, enabled,
            ) {
                onChange(options.copy(voiceComfort = VoiceComfort.entries[it]))
            }
            QualityChoice(
                stringResource(R.string.assist_noise_reduction), strengths, options.noiseReduction.ordinal, enabled,
            ) {
                onChange(options.copy(noiseReduction = NoiseReduction.entries[it]))
            }
            Text(stringResource(R.string.assist_noise_description), style = MaterialTheme.typography.bodySmall)
            QualityChoice(
                stringResource(R.string.assist_quiet_speech), strengths, options.quietSpeech.ordinal,
                enabled && options.noiseReduction != NoiseReduction.OFF,
            ) {
                onChange(options.copy(quietSpeech = QuietSpeech.entries[it]))
            }
            Text(stringResource(R.string.assist_quiet_speech_description), style = MaterialTheme.typography.bodySmall)
            if (!enabled) {
                Text(stringResource(R.string.assist_quality_restart), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun QualityChoice(
    title: String, labels: List<String>, selected: Int, enabled: Boolean, onChange: (Int) -> Unit,
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
internal fun SessionAudioStatus(status: AudioSessionStatus) {
    when (status.state) {
        AudioSessionState.FAILED -> Text(
            status.message ?: stringResource(R.string.assist_connection_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        AudioSessionState.RUNNING -> {
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
