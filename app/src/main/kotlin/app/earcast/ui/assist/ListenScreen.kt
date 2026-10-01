@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.assist.LiveAudioService
import app.earcast.common.AudioLimits
import app.earcast.core.audio.InputSource
import app.earcast.core.audio.dsp.ListeningPreset
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionPage
import app.earcast.ui.common.AdaptiveChoices
import app.earcast.ui.common.AdaptiveSplit
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.SectionHeader
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone
import app.earcast.ui.common.headphonesConnected
import app.earcast.ui.demo.SoundPreviewCard
import kotlinx.coroutines.launch

/**
 * Assist-mode screen: turn on real-time amplification using the saved profile.
 * Requests microphone (and notification) permission, then starts the foreground
 * [LiveAudioService]. A large Stop control is always available (instant off); the
 * amplification slider applies live while running. Warns before starting on the
 * phone speaker, where mic->speaker feedback (howl) is likely.
 */
@Composable
fun ListenScreen(
    onOpenProfile: () -> Unit,
    viewModel: ListenStateModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var speakerWarning by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    val permissions =
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    fun proceedStart() {
        speakerWarning = false
        permissionDenied = false
        scope.launch { if (viewModel.prepare()) LiveAudioService.start(context) }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
            permissionDenied = !micGranted
            if (micGranted) {
                if (headphonesConnected(context)) proceedStart() else speakerWarning = true
            }
        }

    fun startAssist() {
        val micGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        when {
            !micGranted -> launcher.launch(permissions)
            !headphonesConnected(context) -> speakerWarning = true
            else -> proceedStart()
        }
    }

    if (speakerWarning) SpeakerConfirmation(onConfirm = ::proceedStart, onCancel = { speakerWarning = false })

    ActionPage(showActions = state.hasProfile && !state.active, actions = {
        if (state.hasProfile && !state.active) {
            ActionButton(
                stringResource(R.string.ui_start_listening),
                onClick = ::startAssist,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.PlayArrow,
            )
        }
    }) {
        ListeningContent(state, viewModel, onOpenProfile, permissionDenied)
    }
}

@Composable
private fun EmptyAssistState(onOpenProfile: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), tone = SurfaceTone.WARM) {
        SectionHeader(stringResource(R.string.home_profile_needed), index = "01")
        Text(
            stringResource(R.string.assist_no_profile),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        ActionButton(
            label = stringResource(R.string.profile_card_action),
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
    }
}

@Composable
private fun AssistControls(
    state: ListenState,
    viewModel: ListenStateModel,
) {
    SessionAudioStatus(state.sessionStatus)
    if (state.active) ListeningMeterCard(exposure = state.exposure, running = state.running)
    AdaptiveSplit(primary = {
        MicrophoneSelector(state.microphoneSource, !state.active, viewModel::setMicrophoneSource)
        PresetSelector(state.preset, state.active, viewModel::setPreset)
        DetailSection(title = stringResource(R.string.ui_preview)) { SoundPreviewCard() }
    }, secondary = {
        SurfaceCard(Modifier.fillMaxWidth()) {
            DetailSection(title = stringResource(R.string.ui_tuning), initiallyExpanded = true) {
                if (state.active) {
                    Text(
                        stringResource(R.string.ui_stop_to_tune),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SoundOptionsPanel(state.listeningOptions, !state.active, viewModel::setListeningOptions)
            }
        }
    })
}

@Composable
internal fun AmplificationCard(
    state: ListenState,
    onOpenProfile: () -> Unit,
    onGainChange: (Double) -> Unit,
) {
    SurfaceCard(Modifier.fillMaxWidth(), tone = SurfaceTone.TINT) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ui_profile_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    state.profiles
                        .firstOrNull { it.id == state.activeProfileId }
                        ?.name
                        .orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            IconButton(onClick = onOpenProfile) {
                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.ui_profile_manage))
            }
        }
        GainControl(state.masterGainDb, onGainChange)
    }
}

@Composable
private fun MicrophoneSelector(
    source: InputSource,
    enabled: Boolean,
    onChange: (InputSource) -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.assist_microphone_title), style = MaterialTheme.typography.labelLarge)
        Text(
            when (source) {
                InputSource.PHONE -> stringResource(R.string.assist_phone_microphone_desc)
                InputSource.HEADSET -> stringResource(R.string.assist_headset_microphone_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        AdaptiveChoices(
            labels =
                listOf(
                    stringResource(R.string.assist_phone_microphone),
                    stringResource(R.string.assist_headset_microphone),
                ),
            selected = source.ordinal,
            onChange = { onChange(InputSource.entries[it]) },
            enabled = enabled,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (!enabled) {
            Text(
                stringResource(R.string.assist_microphone_while_running),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PresetSelector(
    preset: ListeningPreset,
    running: Boolean,
    onChange: (ListeningPreset) -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.identity_preset_section))
        AdaptiveChoices(
            labels = ListeningPreset.entries.map { presetLabel(it) },
            selected = preset.ordinal,
            onChange = { onChange(ListeningPreset.entries[it]) },
            enabled = !running,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (running) {
            Text(
                stringResource(R.string.ui_stop_to_tune),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun presetLabel(preset: ListeningPreset): String =
    when (preset) {
        ListeningPreset.STANDARD -> stringResource(R.string.preset_standard)
        ListeningPreset.CONVERSATION -> stringResource(R.string.preset_conversation)
    }

@Composable
private fun GainControl(
    masterGainDb: Double,
    onChange: (Double) -> Unit,
) {
    val sliderDescription = stringResource(R.string.assist_amplification_slider)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.ui_amplification),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Text("${masterGainDb.toInt()}", style = MaterialTheme.typography.headlineLarge)
        Text(
            "dB",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Slider(
        value = masterGainDb.toFloat(),
        onValueChange = { onChange(it.toDouble()) },
        valueRange =
            AudioLimits.MIN_MASTER_GAIN_DB.toFloat()..AudioLimits.MAX_MASTER_GAIN_CAP_DB.toFloat(),
        modifier = Modifier.semantics { contentDescription = sliderDescription },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text(
            stringResource(R.string.assist_gain_quieter),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.assist_gain_louder),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SafetyNote() {
    CollapsibleNotice(
        title = stringResource(R.string.notice_summary),
        body = stringResource(R.string.assist_safety_note),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    )
}

@Composable
private fun SpeakerConfirmation(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ui_speaker_title)) },
        text = { Text(stringResource(R.string.assist_speaker_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.assist_start_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.ui_cancel)) }
        },
    )
}

@Composable
private fun ListeningContent(
    state: ListenState,
    viewModel: ListenStateModel,
    onOpenProfile: () -> Unit,
    permissionDenied: Boolean,
) {
    val context = LocalContext.current
    if (!state.hasProfile) {
        EmptyAssistState(onOpenProfile)
    } else {
        AmplificationCard(state, onOpenProfile, viewModel::setMasterGain)
        if (permissionDenied) {
            SurfaceCard(Modifier.fillMaxWidth(), tone = SurfaceTone.WARM) {
                Text(stringResource(R.string.ui_mic_permission), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:" + context.packageName),
                        ),
                    )
                }) { Text(stringResource(R.string.ui_permissions)) }
            }
        }
        AssistControls(state, viewModel)
    }
    SafetyNote()
}
