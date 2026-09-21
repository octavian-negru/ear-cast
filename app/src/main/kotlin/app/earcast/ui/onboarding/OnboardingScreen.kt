@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.JourneyStep
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SoundOrbit

@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    EarPage(modifier = Modifier.safeDrawingPadding()) {
        BrandBar(stringResource(R.string.identity_private))
        SoundOrbit(Modifier.fillMaxWidth().height(128.dp))
        PageHeading(stringResource(R.string.identity_welcome_title), stringResource(R.string.identity_welcome_detail))
        JourneyStep("01", stringResource(R.string.setup_step_one_title), stringResource(R.string.setup_step_one_detail))
        JourneyStep("02", stringResource(R.string.setup_step_two_title), stringResource(R.string.setup_step_two_detail))
        CollapsibleNotice(
            title = stringResource(R.string.disclaimer_title),
            body = stringResource(R.string.disclaimer_body),
            modifier = Modifier.fillMaxWidth(),
            initiallyExpanded = true,
        )
        Text(stringResource(R.string.onboarding_ack), style = MaterialTheme.typography.bodyMedium)
        ActionButton(stringResource(R.string.onboarding_agree), onAccept, Modifier.fillMaxWidth())
    }
}
