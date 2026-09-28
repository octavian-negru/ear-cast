@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import app.earcast.BuildConfig
import app.earcast.R

/** Available before consent and from Settings, including when offline. */
@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_policy_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.privacy_policy_body))
                if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                    Text(stringResource(R.string.privacy_support, BuildConfig.SUPPORT_EMAIL))
                }
                if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                        Text(stringResource(R.string.privacy_policy_online))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.terms_close)) }
        },
    )
}
