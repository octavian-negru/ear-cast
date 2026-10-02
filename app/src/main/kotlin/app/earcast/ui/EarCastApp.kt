@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.assist.LiveAudioService
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.ui.assist.ListenScreen
import app.earcast.ui.assist.ListenStateModel
import app.earcast.ui.background.BackgroundSetupPrompt
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.ChoiceChip
import app.earcast.ui.common.InlineChoices
import app.earcast.ui.common.PageHeading
import app.earcast.ui.dintest.SpeechCheckScreen
import app.earcast.ui.hearingtest.ToneCheckScreen
import app.earcast.ui.manualentry.ProfileEditorScreen
import app.earcast.ui.media.MediaScreen
import app.earcast.ui.navigation.PrimaryDestination
import app.earcast.ui.navigation.navigationBarContent
import app.earcast.ui.navigation.primaryNavigationRail
import app.earcast.ui.onboarding.OnboardingScreen
import app.earcast.ui.profile.ProfileSetupScreen
import app.earcast.ui.settings.SettingsScreen
import app.earcast.ui.theme.EarCastTheme

private enum class AppDestination { MEDIA, PROFILE, HEARING_TEST, MANUAL_ENTRY, ASSIST, DIN_TEST, SETTINGS }

@Composable
fun EarCastApp(rootViewModel: AppStateModel = hiltViewModel()) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()
    val backgroundReviewed by rootViewModel.backgroundSetupReviewed.collectAsStateWithLifecycle()

    EarCastTheme(highContrast = root.highContrast) {
        val colors = MaterialTheme.colorScheme
        if (root.consentAccepted == true) {
            BackgroundSetupPrompt(backgroundReviewed, rootViewModel::markBackgroundSetupReviewed)
        }
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            when (root.consentAccepted) {
                null -> LoadingState()
                false -> OnboardingScreen(onAccept = rootViewModel::acceptDisclaimer)
                true ->
                    MainNav(
                        state = root,
                        onSetMediaEq = rootViewModel::setMediaEq,
                        onSetMediaBoost = rootViewModel::setMediaBoost,
                        onSetMediaProcessingMode = rootViewModel::setMediaProcessingMode,
                    )
            }
        }
    }
}

@Composable
private fun MainNav(
    state: AppState,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
    onSetMediaProcessingMode: (MediaProcessingMode) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(AppDestination.ASSIST) }
    val listenViewModel: ListenStateModel = hiltViewModel()
    val listening by listenViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler(enabled = screen != AppDestination.ASSIST) {
        screen = if (screen.isProfileSetupFlow()) AppDestination.PROFILE else AppDestination.ASSIST
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp && screen.showsPrimaryNavigation()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    if (listening.active) {
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(
                                        if (listening.running) {
                                            R.string.ui_session_active
                                        } else {
                                            R.string.assist_connecting
                                        },
                                    ),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                ActionButton(
                                    stringResource(R.string.assist_stop_button),
                                    { LiveAudioService.stop(context) },
                                    style = ActionStyle.DANGER,
                                )
                            }
                        }
                    }
                    if (!useRail && screen.showsPrimaryNavigation()) {
                        navigationBarContent(
                            selected = screen.primaryDestination(),
                            onDestinationSelected = { destination -> screen = destination.toAppDestination() },
                        )
                    } else {
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            },
        ) { innerPadding ->
            Row(Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    primaryNavigationRail(
                        selected = screen.primaryDestination(),
                        onDestinationSelected = { destination -> screen = destination.toAppDestination() },
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = if (useRail) 920.dp else 720.dp).fillMaxWidth().fillMaxHeight()) {
                        DestinationContent(
                            screen = screen,
                            state = state,
                            listenViewModel = listenViewModel,
                            onNavigate = { screen = it },
                            onSetMediaEq = onSetMediaEq,
                            onSetMediaBoost = onSetMediaBoost,
                            onSetMediaProcessingMode = onSetMediaProcessingMode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationContent(
    screen: AppDestination,
    state: AppState,
    listenViewModel: ListenStateModel,
    onNavigate: (AppDestination) -> Unit,
    onSetMediaEq: (Boolean) -> Unit,
    onSetMediaBoost: (Float) -> Unit,
    onSetMediaProcessingMode: (MediaProcessingMode) -> Unit,
) {
    val listening by listenViewModel.uiState.collectAsStateWithLifecycle()
    when (screen) {
        AppDestination.MEDIA, AppDestination.ASSIST ->
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PageHeading(stringResource(R.string.nav_listen), stringResource(R.string.ui_listen_subtitle))
                    InlineChoices {
                        ChoiceChip(
                            stringResource(R.string.ui_live),
                            screen == AppDestination.ASSIST,
                            { onNavigate(AppDestination.ASSIST) },
                            Modifier.weight(1f),
                        )
                        ChoiceChip(
                            stringResource(R.string.ui_media),
                            screen == AppDestination.MEDIA,
                            { onNavigate(AppDestination.MEDIA) },
                            Modifier.weight(1f),
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    if (screen == AppDestination.ASSIST) {
                        ListenScreen(
                            onOpenProfile = { onNavigate(AppDestination.PROFILE) },
                            viewModel = listenViewModel,
                        )
                    } else {
                        MediaScreen(
                            state = state,
                            onOpenProfile = { onNavigate(AppDestination.PROFILE) },
                            onSetMediaEq = onSetMediaEq,
                            onSetMediaBoost = onSetMediaBoost,
                            onSetMediaProcessingMode = onSetMediaProcessingMode,
                        )
                    }
                }
            }
        AppDestination.PROFILE ->
            ProfileSetupScreen(
                listening = listening,
                onSelectProfile = listenViewModel::selectProfile,
                onDeleteProfile = listenViewModel::deleteProfile,
                state = state,
                onRunHearingTest = { onNavigate(AppDestination.HEARING_TEST) },
                onManualEntry = { onNavigate(AppDestination.MANUAL_ENTRY) },
                onRunDinTest = { onNavigate(AppDestination.DIN_TEST) },
            )
        AppDestination.HEARING_TEST ->
            ToneCheckScreen(
                onBack = { onNavigate(AppDestination.PROFILE) },
                onManualEntry = { onNavigate(AppDestination.MANUAL_ENTRY) },
            )
        AppDestination.MANUAL_ENTRY -> ProfileEditorScreen(onBack = { onNavigate(AppDestination.PROFILE) })
        AppDestination.DIN_TEST -> SpeechCheckScreen(onBack = { onNavigate(AppDestination.PROFILE) })
        AppDestination.SETTINGS -> SettingsScreen()
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.loading_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private fun AppDestination.isProfileSetupFlow(): Boolean =
    this == AppDestination.HEARING_TEST || this == AppDestination.MANUAL_ENTRY || this == AppDestination.DIN_TEST

private fun AppDestination.showsPrimaryNavigation(): Boolean =
    this == AppDestination.MEDIA ||
        this == AppDestination.PROFILE ||
        this == AppDestination.ASSIST ||
        this == AppDestination.SETTINGS

private fun AppDestination.primaryDestination(): PrimaryDestination =
    when (this) {
        AppDestination.MEDIA -> PrimaryDestination.ASSIST
        AppDestination.ASSIST -> PrimaryDestination.ASSIST
        AppDestination.SETTINGS -> PrimaryDestination.SETTINGS
        AppDestination.PROFILE,
        AppDestination.HEARING_TEST,
        AppDestination.MANUAL_ENTRY,
        AppDestination.DIN_TEST,
        -> PrimaryDestination.PROFILE
    }

private fun PrimaryDestination.toAppDestination(): AppDestination =
    when (this) {
        PrimaryDestination.ASSIST -> AppDestination.ASSIST
        PrimaryDestination.PROFILE -> AppDestination.PROFILE
        PrimaryDestination.SETTINGS -> AppDestination.SETTINGS
    }
