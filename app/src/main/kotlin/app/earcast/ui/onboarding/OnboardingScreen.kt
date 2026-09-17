@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.SoundMark
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioPage
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSectionLabel
import app.earcast.ui.common.StudioTone

@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    StudioPage {
        SoundMark()
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.welcome_detail),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StudioPanel(tone = StudioTone.TINT, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            StudioSectionLabel(stringResource(R.string.onboarding_private_title), index = "01")
            Text(
                stringResource(R.string.onboarding_private_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            StudioSectionLabel(
                stringResource(R.string.onboarding_personal_title),
                index = "02",
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.onboarding_personal_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        CollapsibleNotice(
            title = stringResource(R.string.disclaimer_title),
            body = stringResource(R.string.disclaimer_body),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            initiallyExpanded = true,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            stringResource(R.string.onboarding_ack),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        StudioButton(
            label = stringResource(R.string.onboarding_agree),
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
