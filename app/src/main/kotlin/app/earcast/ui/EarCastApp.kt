@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.ui.assist.ListenScreen
import app.earcast.ui.dintest.SpeechCheckScreen
import app.earcast.ui.hearingtest.ToneCheckScreen
import app.earcast.ui.home.HomeScreen
import app.earcast.ui.manualentry.ProfileEditorScreen
import app.earcast.ui.navigation.PrimaryDestination
import app.earcast.ui.navigation.navigationBarContent
import app.earcast.ui.onboarding.OnboardingScreen
import app.earcast.ui.profile.ProfileSetupScreen
import app.earcast.ui.settings.SettingsScreen
import app.earcast.ui.theme.EarCastTheme

private enum class AppDestination { HOME, PROFILE, HEARING_TEST, MANUAL_ENTRY, ASSIST, DIN_TEST, SETTINGS }

@Composable
fun EarCastApp(rootViewModel: AppStateModel = hiltViewModel()) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()

    EarCastTheme(highContrast = root.highContrast) {
        val colors = MaterialTheme.colorScheme
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(colors.surface, colors.background))),
        ) {
            when (root.consentAccepted) {
                null -> LoadingState()
                false -> OnboardingScreen(onAccept = rootViewModel::acceptDisclaimer)
                true ->
                    MainNav(
                        state = root,
                        onSetMediaEq = rootViewModel::setMediaEq,
                        onSetMediaBoost = rootViewModel::setMediaBoost,
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
) {
    var screen by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    BackHandler(enabled = screen != AppDestination.HOME) {
        screen = if (screen.isProfileSetupFlow()) AppDestination.PROFILE else AppDestination.HOME
    }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (screen.showsPrimaryNavigation()) {
                navigationBarContent(
                    selected = screen.primaryDestination(),
                    onDestinationSelected = { destination -> screen = destination.toAppDestination() },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight()) {
                when (screen) {
                    AppDestination.HOME ->
                        HomeScreen(
                            state = state,
                            onOpenProfile = { screen = AppDestination.PROFILE },
                            onOpenAssist = { screen = AppDestination.ASSIST },
                            onSetMediaEq = onSetMediaEq,
                            onSetMediaBoost = onSetMediaBoost,
                        )
                    AppDestination.PROFILE ->
                        ProfileSetupScreen(
                            hasProfile = state.hasProfile,
                            showDinTest = state.dinAvailable,
                            onRunHearingTest = { screen = AppDestination.HEARING_TEST },
                            onManualEntry = { screen = AppDestination.MANUAL_ENTRY },
                            onRunDinTest = { screen = AppDestination.DIN_TEST },
                        )
                    AppDestination.HEARING_TEST ->
                        ToneCheckScreen(
                            onBack = { screen = AppDestination.PROFILE },
                            onManualEntry = { screen = AppDestination.MANUAL_ENTRY },
                        )
                    AppDestination.MANUAL_ENTRY -> ProfileEditorScreen(onBack = { screen = AppDestination.PROFILE })
                    AppDestination.ASSIST ->
                        ListenScreen(
                            onBack = { screen = AppDestination.HOME },
                            onOpenProfile = { screen = AppDestination.PROFILE },
                        )
                    AppDestination.DIN_TEST -> SpeechCheckScreen(onBack = { screen = AppDestination.PROFILE })
                    AppDestination.SETTINGS -> SettingsScreen()
                }
            }
        }
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
    this == AppDestination.HOME ||
        this == AppDestination.PROFILE ||
        this == AppDestination.ASSIST ||
        this == AppDestination.SETTINGS

private fun AppDestination.primaryDestination(): PrimaryDestination =
    when (this) {
        AppDestination.HOME -> PrimaryDestination.HOME
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
        PrimaryDestination.HOME -> AppDestination.HOME
        PrimaryDestination.ASSIST -> AppDestination.ASSIST
        PrimaryDestination.PROFILE -> AppDestination.PROFILE
        PrimaryDestination.SETTINGS -> AppDestination.SETTINGS
    }
