@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SectionGroup
import app.earcast.ui.common.SurfaceCard

@Composable
fun SettingsScreen(rootViewModel: AppStateModel = hiltViewModel()) {
    val state by rootViewModel.uiState.collectAsStateWithLifecycle()
    EarPage {
        BrandBar(stringResource(R.string.nav_settings))
        PageHeading(stringResource(R.string.identity_settings_title), stringResource(R.string.identity_settings_detail))
        SectionGroup(stringResource(R.string.identity_settings_accessibility), index = "01") {
            AppearanceControl(state.highContrast, rootViewModel::setHighContrast)
        }
        SectionGroup(stringResource(R.string.identity_settings_comfort), index = "02") {
            ComfortCalibration(state.comfortCeiling, rootViewModel::setComfortCeiling) {
                rootViewModel.previewComfort(state.comfortCeiling)
            }
        }
        SectionGroup(stringResource(R.string.identity_settings_about), index = "03") {
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleLarge,
            )
            CollapsibleNotice(stringResource(R.string.about_title), stringResource(R.string.about_body))
            CollapsibleNotice(stringResource(R.string.disclaimer_title), stringResource(R.string.disclaimer_body))
        }
    }
}

@Composable
private fun AppearanceControl(
    highContrast: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val label = stringResource(R.string.settings_high_contrast)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.identity_contrast_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(highContrast, onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun ComfortCalibration(
    ceiling: Float,
    onChange: (Float) -> Unit,
    onPreview: () -> Unit,
) {
    val label = stringResource(R.string.settings_comfort_slider)
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${(ceiling * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.identity_comfort_cap),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Slider(
            ceiling,
            onChange,
            valueRange = 0.1f..0.9f,
            modifier = Modifier.semantics { contentDescription = label },
        )
        Text(
            stringResource(R.string.settings_comfort_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        ActionButton(
            stringResource(R.string.settings_comfort_preview),
            onPreview,
            Modifier.fillMaxWidth(),
            style = ActionStyle.SECONDARY,
        )
    }
}
