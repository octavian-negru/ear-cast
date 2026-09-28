@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.InlineChoices
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SectionGroup
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

    EarPage {
        BrandBar(stringResource(R.string.nav_listen))
        PageHeading(stringResource(R.string.identity_listen_title))

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
        SafetyNote()
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
    speakerWarning: Boolean,
    viewModel: ListenStateModel,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    if (speakerWarning && !state.active) SpeakerWarning()

    ListeningConsole(
        state = state,
        onGainChange = viewModel::setMasterGain,
        onStart = onStart,
        onStop = onStop,
        speakerWarning = speakerWarning,
    )

    SessionAudioStatus(state.sessionStatus)

    if (state.active) ListeningMeterCard(exposure = state.exposure, running = state.running)

    SectionGroup(stringResource(R.string.identity_input_section)) {
        MicrophoneSelector(
            source = state.microphoneSource,
            enabled = !state.active,
            onChange = viewModel::setMicrophoneSource,
        )
    }

    PresetSelector(
        preset = state.preset,
        running = state.active,
        onChange = viewModel::setPreset,
    )

    DetailSection(title = stringResource(R.string.assist_more_options)) {
        SoundOptionsPanel(
            options = state.listeningOptions,
            proOwned = state.proOwned,
            enabled = !state.active,
            onChange = viewModel::setListeningOptions,
        )

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
internal fun ListeningConsole(
    state: ListenState,
    speakerWarning: Boolean,
    onGainChange: (Double) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val active = state.active
    val action =
        stringResource(
            if (active) {
                R.string.assist_stop_button
            } else if (speakerWarning) {
                R.string.assist_start_anyway
            } else {
                R.string.assist_start
            },
        )
    val status =
        stringResource(
            when (state.sessionStatus.state) {
                StreamPhase.CONNECTING -> R.string.assist_connecting
                StreamPhase.RUNNING -> R.string.identity_listen_running
                else -> R.string.identity_listen_ready
            },
        )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListeningPowerControl(active, action, if (active) onStop else onStart)
        Text(status, style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.identity_listen_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
    SurfaceCard(Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.identity_sound_controls))
        GainControl(masterGainDb = state.masterGainDb, onChange = onGainChange)
    }
}

@Composable
private fun ListeningPowerControl(
    active: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    ActionButton(
        label = action,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        style = if (active) ActionStyle.DANGER else ActionStyle.PRIMARY,
        icon = if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow,
    )
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
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.identity_preset_section))
        InlineChoices(modifier = Modifier.padding(top = 10.dp)) {
            ListeningPreset.entries.forEach { entry ->
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
) {
    val sliderDescription = stringResource(R.string.assist_amplification_slider)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
private fun ProfilesCard(
    profiles: List<SoundProfile>,
    activeProfileId: String?,
    running: Boolean,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
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
    SurfaceCard(modifier = Modifier.fillMaxWidth(), tone = SurfaceTone.DANGER) {
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
