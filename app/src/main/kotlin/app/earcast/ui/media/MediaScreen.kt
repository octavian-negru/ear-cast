@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.common.AudioLimits
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.ui.AppState
import app.earcast.ui.ads.TestAdsSection
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ChoiceChip
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.InlineChoices
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone

@Composable
fun MediaScreen(
    state: AppState,
    onOpenProfile: () -> Unit,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
    onSetMediaProcessingMode: (MediaProcessingMode) -> Unit,
) {
    EarPage {
        if (!state.hasProfile) WelcomeInstrument(onOpenProfile)
        MediaPlaybackSection(state, onSetMediaEq, onSetMediaBoost, onSetMediaProcessingMode)
        SafetyDisclaimer()
        TestAdsSection()
    }
}

@Composable
private fun WelcomeInstrument(onOpenProfile: () -> Unit) {
    SurfaceCard(tone = SurfaceTone.TINT, modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Text(
            stringResource(R.string.identity_first_step),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.identity_start_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.identity_start_detail),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        ActionButton(
            stringResource(R.string.profile_card_action),
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SafetyDisclaimer() {
    CollapsibleNotice(
        title = stringResource(R.string.disclaimer_title),
        body = stringResource(R.string.disclaimer_body),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun MediaPlaybackSection(
    state: AppState,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
    onSetMediaProcessingMode: (MediaProcessingMode) -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), tone = SurfaceTone.PLAIN, padding = 16.dp) {
        Text(
            stringResource(R.string.identity_media_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MediaSoundToggle(state, onSetMediaEq)
        Text(
            stringResource(R.string.media_eq_desc),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (state.mediaEqEnabled && state.mediaEqSupported && state.hasProfile) {
            DetailSection(title = stringResource(R.string.media_adjust_sound), initiallyExpanded = true) {
                MediaAlgorithmControl(state, onSetMediaProcessingMode)
                MediaBoostControl(state, onSetMediaBoost)
            }
        }
        MediaSoundStatus(state)
    }
}

@Composable
private fun MediaAlgorithmControl(
    state: AppState,
    onSetMediaProcessingMode: (MediaProcessingMode) -> Unit,
) {
    val enabled = state.mediaEqEnabled && state.mediaEqSupported && state.hasProfile
    Text(
        stringResource(R.string.media_algorithm),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
    InlineChoices {
        ChoiceChip(
            label = stringResource(R.string.media_algorithm_balanced),
            selected = state.mediaProcessingMode == MediaProcessingMode.BALANCED,
            onClick = { onSetMediaProcessingMode(MediaProcessingMode.BALANCED) },
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
        ChoiceChip(
            label = stringResource(R.string.media_algorithm_speech),
            selected = state.mediaProcessingMode == MediaProcessingMode.SPEECH_CLARITY,
            onClick = { onSetMediaProcessingMode(MediaProcessingMode.SPEECH_CLARITY) },
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
    }
    Text(
        stringResource(
            if (state.mediaProcessingMode == MediaProcessingMode.SPEECH_CLARITY) {
                R.string.media_algorithm_speech_desc
            } else {
                R.string.media_algorithm_balanced_desc
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun MediaSoundToggle(
    state: AppState,
    onSetMediaEq: (Boolean) -> Unit,
) {
    val toggleDescription = stringResource(R.string.media_eq_title)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.media_eq_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.mediaEqEnabled,
            onCheckedChange = onSetMediaEq,
            enabled = state.mediaEqSupported && state.hasProfile,
            modifier = Modifier.semantics { contentDescription = toggleDescription },
        )
    }
}

@Composable
private fun MediaBoostControl(
    state: AppState,
    onSetMediaBoost: (Float) -> Unit,
) {
    var boost by rememberSaveable(state.mediaBoostDb) { mutableStateOf(state.mediaBoostDb) }
    val description = stringResource(R.string.media_boost_slider)
    Text(
        stringResource(R.string.media_boost, boost.toInt()),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(
        value = boost,
        onValueChange = { boost = it },
        onValueChangeFinished = { onSetMediaBoost(boost) },
        valueRange = 0f..AudioLimits.MAX_MEDIA_BOOST_DB,
        steps = AudioLimits.MAX_MEDIA_BOOST_DB.toInt() - 1,
        enabled = state.mediaEqEnabled && state.mediaEqSupported && state.hasProfile,
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun MediaSoundStatus(state: AppState) {
    val status =
        when {
            !state.mediaEqSupported -> stringResource(R.string.media_eq_unsupported)
            !state.hasProfile -> stringResource(R.string.media_eq_no_profile)
            state.mediaEqFailed -> stringResource(R.string.media_eq_failed)
            else -> null
        }
    if (status != null) {
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
