@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.earcast.R

/** The same terms are available before consent and later from Settings. */
@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terms_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.terms_version), style = MaterialTheme.typography.labelMedium)
                TermsSection(R.string.terms_purpose_title, R.string.terms_purpose_body)
                TermsSection(R.string.listening_safety_title, R.string.listening_safety_body)
                TermsSection(R.string.terms_limits_title, R.string.terms_limits_body)
                TermsSection(R.string.terms_privacy_title, R.string.terms_privacy_body)
                TermsSection(R.string.terms_agreement_title, R.string.terms_agreement_body)
                SafetySourceLink()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.terms_close)) }
        },
    )
}

@Composable
private fun TermsSection(
    title: Int,
    body: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SafetySourceLink() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.safety_source_url)
    TextButton(onClick = { uriHandler.openUri(url) }) {
        Text(stringResource(R.string.safety_source))
    }
}
