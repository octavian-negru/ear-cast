@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.AppState
import app.earcast.ui.assist.ListenState
import app.earcast.ui.common.AudiogramCard
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.FeatureRoute
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SectionGroup
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone
import app.earcast.ui.share.ProfileShareDialog

@Composable
fun ProfileSetupScreen(
    state: AppState,
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
    onRunDinTest: () -> Unit,
    listening: ListenState = ListenState(),
    onSelectProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
) {
    val audiogram = state.audiogram
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var share by rememberSaveable { mutableStateOf(false) }
    val deleteProfile = listening.profiles.firstOrNull { it.id == deleteId }
    if (deleteProfile != null) {
        DeleteProfileDialog(deleteProfile, !listening.active, onCancel = { deleteId = null }) {
            onDeleteProfile(deleteProfile.id)
            deleteId = null
        }
    }
    if (share && audiogram != null) ProfileShareDialog(audiogram) { share = false }
    EarPage {
        PageHeading(stringResource(R.string.ui_profiles), stringResource(R.string.ui_profile_subtitle))
        if (listening.active) {
            SurfaceCard(Modifier.fillMaxWidth(), tone = SurfaceTone.WARM) {
                Text(stringResource(R.string.ui_stop_to_edit), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.hasProfile) {
            AudiogramCard(
                audiogram,
                onManualEntry,
                stringResource(R.string.profile_editor_action),
                editEnabled = !listening.active,
                onShare = if (audiogram != null) ({ share = true }) else null,
            )
            CollapsibleNotice(
                stringResource(R.string.identity_read_chart),
                stringResource(R.string.audiogram_reading_hint),
            )
        } else {
            Text(stringResource(R.string.identity_two_ways), style = MaterialTheme.typography.titleLarge)
        }
        SavedProfiles(listening, onSelectProfile, onDelete = { deleteId = it })
        SectionGroup(stringResource(R.string.ui_profile_create)) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                FeatureRoute(
                    title = stringResource(R.string.profile_check_title),
                    detail = stringResource(R.string.profile_check_detail),
                    icon = Icons.Filled.Headphones,
                    onClick = onRunHearingTest,
                    enabled = !listening.active,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FeatureRoute(
                    title = stringResource(R.string.profile_editor_title),
                    detail = stringResource(R.string.profile_editor_detail),
                    icon = Icons.Filled.Edit,
                    onClick = onManualEntry,
                    enabled = !listening.active,
                )
            }
        }
        if (state.dinAvailable) {
            FeatureRoute(
                title = stringResource(R.string.home_din_test),
                detail = stringResource(R.string.identity_speech_detail),
                icon = Icons.Filled.RecordVoiceOver,
                onClick = onRunDinTest,
                enabled = !listening.active,
            )
        }
        CollapsibleNotice(stringResource(R.string.notice_summary), stringResource(R.string.disclaimer_body))
    }
}

@Composable
private fun SavedProfiles(
    listening: ListenState,
    onSelectProfile: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (listening.profiles.isNotEmpty()) {
        SectionGroup(stringResource(R.string.profiles_title)) {
            SurfaceCard(Modifier.fillMaxWidth().selectableGroup()) {
                listening.profiles.forEach { profile ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = profile.id == listening.activeProfileId,
                                    enabled = !listening.active,
                                    role = Role.RadioButton,
                                    onClick = { onSelectProfile(profile.id) },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                profile.id == listening.activeProfileId,
                                onClick = null,
                                enabled = !listening.active,
                            )
                            Text(
                                profile.name,
                                Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        TextButton(onClick = { onDelete(profile.id) }, enabled = !listening.active) {
                            Text(stringResource(R.string.profile_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProfileDialog(
    profile: app.earcast.data.SoundProfile,
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ui_profile_delete_title)) },
        text = { Text(stringResource(R.string.ui_profile_delete_body, profile.name)) },
        confirmButton = {
            TextButton(enabled = enabled, onClick = onConfirm) {
                Text(stringResource(R.string.profile_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.ui_cancel)) } },
    )
}
