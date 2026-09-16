@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.BuildConfig
import app.earcast.R
import app.earcast.common.AudioLimits
import app.earcast.ui.assist.ListenScreen
import app.earcast.ui.common.ActionCard
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.PageHeading
import app.earcast.ui.dintest.SpeechCheckScreen
import app.earcast.ui.hearingtest.ToneCheckScreen
import app.earcast.ui.manualentry.ProfileEditorScreen
import app.earcast.ui.theme.EarCastTheme

private enum class AppDestination { HOME, HEARING_TEST, MANUAL_ENTRY, ASSIST, DIN_TEST, SETTINGS }

@Composable
fun EarCastApp(rootViewModel: AppStateModel = hiltViewModel()) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()

    EarCastTheme(highContrast = root.highContrast) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                when (root.consentAccepted) {
                    null -> Unit // loading
                    false -> OnboardingScreen(onAccept = rootViewModel::acceptDisclaimer)
                    true -> MainNav(dinAvailable = root.dinAvailable)
                }
            }
        }
    }
}

@Composable
private fun MainNav(dinAvailable: Boolean) {
    var screen by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    // System back returns to Home from any sub-screen instead of exiting the app.
    BackHandler(enabled = screen != AppDestination.HOME) { screen = AppDestination.HOME }
    when (screen) {
        AppDestination.HOME ->
            HomeScreen(
                onRunHearingTest = { screen = AppDestination.HEARING_TEST },
                onAssist = { screen = AppDestination.ASSIST },
                onDinTest = { screen = AppDestination.DIN_TEST },
                onSettings = { screen = AppDestination.SETTINGS },
                showDinTest = dinAvailable,
            )
        AppDestination.HEARING_TEST ->
            ToneCheckScreen(
                onBack = { screen = AppDestination.HOME },
                onManualEntry = { screen = AppDestination.MANUAL_ENTRY },
            )
        AppDestination.MANUAL_ENTRY -> ProfileEditorScreen(onBack = { screen = AppDestination.HOME })
        AppDestination.ASSIST -> ListenScreen(onBack = { screen = AppDestination.HOME })
        AppDestination.DIN_TEST -> SpeechCheckScreen(onBack = { screen = AppDestination.HOME })
        AppDestination.SETTINGS -> SettingsScreen(onBack = { screen = AppDestination.HOME })
    }
}

@Composable
private fun HomeScreen(
    onRunHearingTest: () -> Unit,
    onAssist: () -> Unit,
    onDinTest: () -> Unit,
    onSettings: () -> Unit,
    showDinTest: Boolean,
    rootViewModel: AppStateModel = hiltViewModel(),
) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        PageHeading(stringResource(R.string.app_name), stringResource(R.string.tagline))
        DisclaimerCard(modifier = Modifier.padding(top = 8.dp))

        Text(
            stringResource(R.string.home_start_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        ActionCard(
            title = stringResource(R.string.home_hearing_check),
            detail = stringResource(R.string.check_intro),
            action = stringResource(R.string.check_start),
            onClick = onRunHearingTest,
        )
        ActionCard(
            title = stringResource(R.string.home_assist),
            detail = stringResource(R.string.listen_card_detail),
            action = stringResource(R.string.listen_card_action),
            onClick = onAssist,
        )

        Text(
            stringResource(R.string.home_tools_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 28.dp),
        )
        if (showDinTest) HomeButton(stringResource(R.string.home_din_test), onDinTest)
        Text(
            stringResource(R.string.home_media_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        MediaEqCard(state = root, onToggle = rootViewModel::setMediaEq, onBoostChange = rootViewModel::setMediaBoost)
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.home_settings))
        }
    }
}

@Composable
private fun HomeButton(
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 12.dp),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun OnboardingScreen(onAccept: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        DisclaimerCard(modifier = Modifier.padding(top = 16.dp), initiallyExpanded = true)
        Text(
            stringResource(R.string.onboarding_ack),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(top = 16.dp),
        ) { Text(stringResource(R.string.onboarding_agree), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    rootViewModel: AppStateModel = hiltViewModel(),
) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_high_contrast), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = root.highContrast, onCheckedChange = rootViewModel::setHighContrast)
        }

        ComfortCalibration(
            ceiling = root.comfortCeiling,
            onChange = rootViewModel::setComfortCeiling,
            onPreview = { rootViewModel.previewComfort(root.comfortCeiling) },
        )

        AboutCard()

        DisclaimerCard(modifier = Modifier.padding(top = 24.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun ComfortCalibration(
    ceiling: Float,
    onChange: (Float) -> Unit,
    onPreview: () -> Unit,
) {
    val sliderDescription = stringResource(R.string.settings_comfort_slider)
    Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
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
private fun MediaEqCard(
    state: AppState,
    onToggle: (Boolean) -> Unit,
    onBoostChange: (Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.media_eq_title), style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.mediaEqEnabled,
                    onCheckedChange = onToggle,
                    enabled = state.mediaEqSupported && state.hasProfile,
                )
            }
            Text(
                stringResource(R.string.media_eq_desc),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            MediaBoostControl(state = state, onChange = onBoostChange)
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
    }
}

@Composable
private fun MediaBoostControl(
    state: AppState,
    onChange: (Float) -> Unit,
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
        onValueChangeFinished = { onChange(boost) },
        valueRange = 0f..AudioLimits.MAX_MEDIA_BOOST_DB,
        steps = AudioLimits.MAX_MEDIA_BOOST_DB.toInt() - 1,
        enabled = state.mediaEqEnabled && state.mediaEqSupported && state.hasProfile,
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DisclaimerCard(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    CollapsibleNotice(
        title = stringResource(R.string.disclaimer_title),
        body = stringResource(R.string.disclaimer_body),
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}
