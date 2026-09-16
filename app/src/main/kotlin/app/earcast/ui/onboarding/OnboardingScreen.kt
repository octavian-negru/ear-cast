@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.PageHeading

@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        PageHeading(stringResource(R.string.app_name), stringResource(R.string.onboarding_welcome))
        CollapsibleNotice(
            title = stringResource(R.string.disclaimer_title),
            body = stringResource(R.string.disclaimer_body),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            initiallyExpanded = true,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            stringResource(R.string.onboarding_ack),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.onboarding_agree), style = MaterialTheme.typography.labelLarge)
        }
    }
}
