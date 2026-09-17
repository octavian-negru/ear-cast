@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import app.earcast.core.audio.StreamPhase
import app.earcast.core.audio.dsp.ListeningPreset
import app.earcast.data.SoundProfile
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPage
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSectionLabel
import app.earcast.ui.common.StudioSplitRow
import app.earcast.ui.common.StudioStatus
import app.earcast.ui.common.StudioTone
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
    var speakerWarning by remember { mutableStateOf(false) }

    val permissions =
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    fun proceedStart() {
        speakerWarning = false
        scope.launch { if (viewModel.prepare()) LiveAudioService.start(context) }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
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
            !headphonesConnected(context) && !speakerWarning -> speakerWarning = true
            else -> proceedStart()
        }
    }

    StudioPage {
        PageHeading(stringResource(R.string.assist_title), stringResource(R.string.listen_subtitle))
        SafetyNote()

        if (!state.hasProfile) {
            EmptyAssistState(onOpenProfile)
        } else {
            AssistControls(
                state = state,
                speakerWarning = speakerWarning,
                viewModel = viewModel,
                onStart = ::startAssist,
                onStop = { LiveAudioService.stop(context) },
            )
        }
    }
}

@Composable
private fun EmptyAssistState(onOpenProfile: () -> Unit) {
    StudioPanel(modifier = Modifier.fillMaxWidth(), tone = StudioTone.WARM) {
        StudioSectionLabel(stringResource(R.string.home_profile_needed), index = "01")
        Text(
            stringResource(R.string.assist_no_profile),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        StudioButton(
            label = stringResource(R.string.profile_card_action),
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
    }
}

@Composable
private fun AssistControls(
    state: ListenState,
    speakerWarning: Boolean,
    viewModel: ListenStateModel,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    AssistConsole(
        state = state,
        onGainChange = viewModel::setMasterGain,
        onStart = onStart,
        onStop = onStop,
        speakerWarning = speakerWarning,
    )

    SessionAudioStatus(state.sessionStatus)

    if (speakerWarning && !state.active) SpeakerWarning()

    ListeningMeterCard(exposure = state.exposure, running = state.running)

    StudioSectionLabel(stringResource(R.string.assist_signal_path), modifier = Modifier.padding(top = 4.dp))
    MicrophoneSelector(
        source = state.microphoneSource,
        enabled = !state.active,
        onChange = viewModel::setMicrophoneSource,
    )

    PresetSelector(
        preset = state.preset,
        running = state.active,
        onChange = viewModel::setPreset,
    )

    DetailSection(title = stringResource(R.string.assist_more_options)) {
        SoundOptionsPanel(
            options = state.listeningOptions,
            enabled = !state.active,
            onChange = viewModel::setListeningOptions,
        )
        RecordingPanel(viewModel, state.active)

        if (state.profiles.size > 1) {
            ProfilesCard(
                profiles = state.profiles,
                activeProfileId = state.activeProfileId,
                running = state.active,
                onSelect = viewModel::selectProfile,
                onDelete = viewModel::deleteProfile,
            )
        }

        SoundPreviewCard()
    }
}

@Composable
private fun AssistConsole(
    state: ListenState,
    speakerWarning: Boolean,
    onGainChange: (Double) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val active = state.active
    val status =
        when (state.sessionStatus.state) {
            StreamPhase.CONNECTING -> stringResource(R.string.assist_connecting)
            StreamPhase.RUNNING -> stringResource(R.string.assist_on)
            else -> stringResource(R.string.assist_off)
        }
    StudioPanel(
        modifier = Modifier.fillMaxWidth(),
        tone = if (active) StudioTone.DARK else StudioTone.TINT,
        padding = 18.dp,
    ) {
        StudioStatus(label = status, active = active)
        Text(
            stringResource(if (active) R.string.assist_console_live else R.string.assist_console_ready),
            style = MaterialTheme.typography.headlineSmall,
            color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            stringResource(if (active) R.string.assist_console_live_detail else R.string.assist_console_ready_detail),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (active) {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.74f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                },
            modifier = Modifier.padding(top = 5.dp),
        )
        GainControl(
            masterGainDb = state.masterGainDb,
            onChange = onGainChange,
            lightContent = active,
        )
        StudioButton(
            label =
                if (active) {
                    stringResource(R.string.assist_stop_button)
                } else if (speakerWarning) {
                    stringResource(R.string.assist_start_anyway)
                } else {
                    stringResource(R.string.assist_start)
                },
            onClick = if (active) onStop else onStart,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            style = if (active) StudioButtonStyle.DANGER else StudioButtonStyle.PRIMARY,
        )
    }
}

@Composable
private fun MicrophoneSelector(
    source: InputSource,
    enabled: Boolean,
    onChange: (InputSource) -> Unit,
) {
    StudioPanel(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.assist_microphone_title), style = MaterialTheme.typography.labelLarge)
        Text(
            when (source) {
                InputSource.PHONE -> stringResource(R.string.assist_phone_microphone_desc)
                InputSource.HEADSET -> stringResource(R.string.assist_headset_microphone_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        InputSource.entries.forEach { option ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = option == source,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onChange(option) },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionDot(selected = option == source)
                Text(
                    when (option) {
                        InputSource.PHONE -> stringResource(R.string.assist_phone_microphone)
                        InputSource.HEADSET -> stringResource(R.string.assist_headset_microphone)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
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
    StudioPanel(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.preset_title), style = MaterialTheme.typography.labelLarge)
        StudioSplitRow(modifier = Modifier.padding(top = 10.dp)) {
            ListeningPreset.entries.forEachIndexed { index, entry ->
                PresetTile(
                    label = presetLabel(entry),
                    selected = entry == preset,
                    onClick = { onChange(entry) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (running) {
            Text(
                stringResource(R.string.preset_while_running),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PresetTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .heightIn(min = 50.dp)
                .clip(MaterialTheme.shapes.small)
                .background(containerColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    val fillColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    Box(
        modifier =
            Modifier
                .padding(horizontal = 6.dp)
                .size(18.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(fillColor),
    )
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
    lightContent: Boolean,
) {
    val sliderDescription = stringResource(R.string.assist_amplification_slider)
    val contentColor =
        if (lightContent) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer
    val scaleColor =
        if (lightContent) {
            MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(
        stringResource(R.string.assist_amplification, masterGainDb.toInt()),
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        modifier = Modifier.padding(top = 18.dp),
    )
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
            color = scaleColor,
        )
        Text(
            stringResource(R.string.assist_gain_louder),
            style = MaterialTheme.typography.labelMedium,
            color = scaleColor,
        )
    }
}

@Composable
private fun ProfilesCard(
    profiles: List<SoundProfile>,
    activeProfileId: String?,
    running: Boolean,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    StudioPanel(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.titleSmall)
        profiles.forEach { profile ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = profile.id == activeProfileId,
                            role = Role.RadioButton,
                            onClick = { onSelect(profile.id) },
                        ),
            ) {
                SelectionDot(selected = profile.id == activeProfileId)
                Text(
                    profile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                TextButton(onClick = { onDelete(profile.id) }) {
                    Text(stringResource(R.string.profile_delete))
                }
            }
        }
        if (running) {
            Text(
                stringResource(R.string.profile_switch_while_running),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SpeakerWarning() {
    StudioPanel(modifier = Modifier.fillMaxWidth(), tone = StudioTone.DANGER) {
        Text(
            stringResource(R.string.assist_speaker_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
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
