@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.ActionCard
import app.earcast.ui.common.ActionCardEmphasis
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.DetailSection
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPage
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSectionLabel
import app.earcast.ui.common.StudioStatus
import app.earcast.ui.common.StudioTone

@Composable
fun ProfileSetupScreen(
    hasProfile: Boolean,
    showDinTest: Boolean,
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
    onRunDinTest: () -> Unit,
) {
    StudioPage {
        PageHeading(stringResource(R.string.nav_profile), stringResource(R.string.profile_subtitle))
        ProfileStatus(hasProfile)
        ProfileRoutes(onRunHearingTest, onManualEntry)
        if (showDinTest) AdditionalTools(onRunDinTest)
        ProfileNotice()
    }
}

@Composable
private fun ProfileNotice() {
    CollapsibleNotice(
        title = stringResource(R.string.notice_summary),
        body = stringResource(R.string.disclaimer_body),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun ProfileStatus(hasProfile: Boolean) {
    StudioPanel(
        modifier = Modifier.fillMaxWidth(),
        tone = if (hasProfile) StudioTone.TINT else StudioTone.WARM,
    ) {
        StudioStatus(
            label = stringResource(if (hasProfile) R.string.home_profile_ready else R.string.home_profile_needed),
            active = hasProfile,
        )
        Text(
            stringResource(if (hasProfile) R.string.profile_ready else R.string.profile_missing_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun ProfileRoutes(
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
) {
    StudioSectionLabel(stringResource(R.string.profile_setup_section), modifier = Modifier.padding(top = 4.dp))
    ActionCard(
        title = stringResource(R.string.profile_check_title),
        detail = stringResource(R.string.profile_check_detail),
        action = stringResource(R.string.profile_check_action),
        onClick = onRunHearingTest,
    )
    ActionCard(
        title = stringResource(R.string.profile_editor_title),
        detail = stringResource(R.string.profile_editor_detail),
        action = stringResource(R.string.profile_editor_action),
        onClick = onManualEntry,
        emphasis = ActionCardEmphasis.SECONDARY,
    )
}

@Composable
private fun AdditionalTools(onRunDinTest: () -> Unit) {
    DetailSection(title = stringResource(R.string.home_tools_section)) {
        StudioButton(
            label = stringResource(R.string.home_din_test),
            onClick = onRunDinTest,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            style = StudioButtonStyle.SECONDARY,
        )
    }
}
