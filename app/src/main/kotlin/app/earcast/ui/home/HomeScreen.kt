@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import app.earcast.ui.AppState
import app.earcast.ui.common.ActionCard
import app.earcast.ui.common.ActionCardEmphasis
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SoundMark

@Composable
fun HomeScreen(
    state: AppState,
    onOpenProfile: () -> Unit,
    onOpenAssist: () -> Unit,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SoundMark(Modifier.padding(bottom = 4.dp))
        PageHeading(stringResource(R.string.app_name), stringResource(R.string.home_subtitle))
        SafetyDisclaimer()
        HomeActions(state = state, onOpenProfile = onOpenProfile, onOpenAssist = onOpenAssist)
        MediaPlaybackSection(state, onSetMediaEq, onSetMediaBoost)
    }
}

@Composable
private fun SafetyDisclaimer() {
    CollapsibleNotice(
        title = stringResource(R.string.disclaimer_title),
        body = stringResource(R.string.disclaimer_body),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
private fun HomeActions(
    state: AppState,
    onOpenProfile: () -> Unit,
    onOpenAssist: () -> Unit,
) {
    Text(
        stringResource(R.string.home_start_section),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp),
    )
    if (state.hasProfile) {
        ActionCard(
            title = stringResource(R.string.listen_card_title),
            detail = stringResource(R.string.listen_card_detail),
            action = stringResource(R.string.listen_card_action),
            onClick = onOpenAssist,
        )
        ActionCard(
            title = stringResource(R.string.profile_card_title),
            detail = stringResource(R.string.profile_card_detail),
            action = stringResource(R.string.profile_card_action),
            onClick = onOpenProfile,
            emphasis = ActionCardEmphasis.SECONDARY,
        )
    } else {
        ActionCard(
            title = stringResource(R.string.profile_card_title),
            detail = stringResource(R.string.profile_card_detail),
            action = stringResource(R.string.profile_card_action),
            onClick = onOpenProfile,
        )
    }
}

@Composable
private fun MediaPlaybackSection(
    state: AppState,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
) {
    DetailSection(
        title = stringResource(R.string.home_media_section),
        initiallyExpanded = state.mediaEqEnabled || state.mediaEqFailed,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                MediaSoundToggle(state, onSetMediaEq)
                Text(
                    stringResource(R.string.media_eq_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                MediaBoostControl(state, onSetMediaBoost)
                MediaSoundStatus(state)
            }
        }
    }
}

@Composable
private fun MediaSoundToggle(
    state: AppState,
    onSetMediaEq: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.media_eq_title), style = MaterialTheme.typography.titleSmall)
        Switch(
            checked = state.mediaEqEnabled,
            onCheckedChange = onSetMediaEq,
            enabled = state.mediaEqSupported && state.hasProfile,
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
        modifier = Modifier.padding(top = 12.dp),
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
