@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun SettingsScreen(rootViewModel: AppStateModel = hiltViewModel()) {
    val state by rootViewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        PageHeading(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle))
        AppearanceControl(state.highContrast, rootViewModel::setHighContrast)
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
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
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
    Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Column(Modifier.padding(16.dp)) {
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
            OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_comfort_preview))
            }
        }
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
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}
