@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import app.earcast.BuildConfig
import app.earcast.R

/** Available before consent and from Settings, including when offline. */
@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    LegalDocument(stringResource(R.string.privacy_policy_title), onDismiss) {
        Text(
            stringResource(R.string.privacy_policy_body),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
        )
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
            Text(stringResource(R.string.privacy_support, BuildConfig.SUPPORT_EMAIL))
        }
        if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
            TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                Text(stringResource(R.string.privacy_policy_online))
            }
        }
    }
}
