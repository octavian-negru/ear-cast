@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.earcast.audiogram.HearingCurve
import app.earcast.audiogram.HearingPoint
import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import app.earcast.ui.AppState
import app.earcast.ui.assist.ListenState
import app.earcast.ui.assist.ListeningConsole
import app.earcast.ui.common.EarPage
import app.earcast.ui.home.HomeScreen
import app.earcast.ui.navigation.PrimaryDestination
import app.earcast.ui.navigation.navigationBarContent
import app.earcast.ui.theme.EarCastTheme

// Fictional data is confined to IDE previews. Production screens use the active saved profile.
private fun previewAudiogram(): HearingCurve =
    HearingCurve(
        listOf(250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0).flatMapIndexed { index, frequency ->
            listOf(
                HearingPoint(
                    AudioEar.RIGHT,
                    FrequencyHz(frequency),
                    HearingDb(listOf(10.0, 15.0, 15.0, 30.0, 40.0, 35.0)[index]),
                ),
                HearingPoint(
                    AudioEar.LEFT,
                    FrequencyHz(frequency),
                    HearingDb(listOf(15.0, 10.0, 20.0, 25.0, 30.0, 40.0)[index]),
                ),
            )
        },
    )

@Preview(name = "First launch · phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FirstLaunchPreview() = HomePreview(ready = false)

@Preview(name = "Saved audiogram · phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ReadyPreview() = HomePreview(ready = true)

@Preview(name = "Dark · phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DarkPreview() = HomePreview(ready = true, dark = true)

@Preview(name = "Large text · phone", widthDp = 360, heightDp = 900, fontScale = 1.5f)
@Composable
private fun LargeTextPreview() = HomePreview(ready = true)

@Preview(name = "Tablet · overview", widthDp = 1000, heightDp = 900)
@Composable
private fun TabletPreview() = HomePreview(ready = true)

@Preview(name = "Listening control", widthDp = 390, heightDp = 700)
@Composable
private fun ListeningPreview() {
    EarCastTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EarPage {
                ListeningConsole(ListenState(hasProfile = true), false, {}, {}, {})
            }
        }
    }
}

@Composable
private fun HomePreview(
    ready: Boolean,
    dark: Boolean = false,
) {
    EarCastTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    HomeScreen(
                        state = AppState(hasProfile = ready, audiogram = if (ready) previewAudiogram() else null),
                        onOpenProfile = {},
                        onOpenAssist = {},
                        onSetMediaEq = {},
                        onSetMediaBoost = {},
                        onSetMediaProcessingMode = {},
                    )
                }
                navigationBarContent(PrimaryDestination.HOME) {}
            }
        }
    }
}
