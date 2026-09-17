@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.BuildConfig
import app.earcast.R
import app.earcast.ui.AppStateModel
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPage
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSectionLabel
import app.earcast.ui.common.StudioTone

@Composable
fun SettingsScreen(rootViewModel: AppStateModel = hiltViewModel()) {
    val state by rootViewModel.uiState.collectAsStateWithLifecycle()
    StudioPage {
        PageHeading(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle))
        StudioSectionLabel(stringResource(R.string.settings_display_section))
        AppearanceControl(state.highContrast, rootViewModel::setHighContrast)
        StudioSectionLabel(stringResource(R.string.settings_listening_section), modifier = Modifier.padding(top = 4.dp))
        ComfortCalibration(
            ceiling = state.comfortCeiling,
            onChange = rootViewModel::setComfortCeiling,
            onPreview = { rootViewModel.previewComfort(state.comfortCeiling) },
        )
        AboutSection()
        SafetyNotice()
    }
}

@Composable
private fun AppearanceControl(
    highContrast: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.settings_high_contrast), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = highContrast, onCheckedChange = onChange)
    }
}

@Composable
private fun ComfortCalibration(
    ceiling: Float,
    onChange: (Float) -> Unit,
    onPreview: () -> Unit,
) {
    val sliderDescription = stringResource(R.string.settings_comfort_slider)
    StudioPanel(modifier = Modifier.fillMaxWidth(), tone = StudioTone.WARM) {
        Text(stringResource(R.string.settings_comfort_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_comfort_desc),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Slider(
            value = ceiling,
            onValueChange = onChange,
            valueRange = 0.1f..0.9f,
            modifier = Modifier.semantics { contentDescription = sliderDescription },
        )
        StudioButton(
            label = stringResource(R.string.settings_comfort_preview),
            onClick = onPreview,
            modifier = Modifier.fillMaxWidth(),
            style = StudioButtonStyle.SECONDARY,
        )
    }
}

@Composable
private fun AboutSection() {
    CollapsibleNotice(
        title = stringResource(R.string.about_title),
        body =
            "${stringResource(R.string.about_version, BuildConfig.VERSION_NAME)}\n\n" +
                stringResource(R.string.about_body),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
}

@Composable
private fun SafetyNotice() {
    CollapsibleNotice(
        title = stringResource(R.string.disclaimer_title),
        body = stringResource(R.string.disclaimer_body),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
