@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun ProfileSetupScreen(
    hasProfile: Boolean,
    showDinTest: Boolean,
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
    onRunDinTest: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        PageHeading(stringResource(R.string.nav_profile), stringResource(R.string.profile_subtitle))
        ProfileNotice()
        ProfileStatus(hasProfile)
        ProfileRoutes(onRunHearingTest, onManualEntry)
        if (showDinTest) AdditionalTools(onRunDinTest)
    }
}

@Composable
private fun ProfileNotice() {
    CollapsibleNotice(
        title = stringResource(R.string.notice_summary),
        body = stringResource(R.string.disclaimer_body),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
private fun ProfileStatus(hasProfile: Boolean) {
    if (!hasProfile) return
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            stringResource(R.string.profile_ready),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ProfileRoutes(
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
) {
    Text(
        stringResource(R.string.profile_setup_section),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp),
    )
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
        OutlinedButton(
            onClick = onRunDinTest,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.home_din_test), style = MaterialTheme.typography.labelLarge)
        }
    }
}
