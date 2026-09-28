@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.earcast.R
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SurfaceCard
import app.earcast.ui.common.SurfaceTone

@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    var understandsLimitations by rememberSaveable { mutableStateOf(false) }
    var agreesToTerms by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showTerms by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        EarPage(modifier = Modifier.widthIn(max = 600.dp)) {
            BrandBar(stringResource(R.string.identity_private))
            PageHeading(
                stringResource(R.string.identity_welcome_title),
                stringResource(R.string.identity_welcome_detail),
            )
            SurfaceCard(modifier = Modifier.fillMaxWidth(), tone = SurfaceTone.TINT) {
                Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.terms_purpose_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(stringResource(R.string.listening_safety_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.listening_safety_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showPrivacy = true }) { Text(stringResource(R.string.privacy_policy_title)) }
            TextButton(onClick = { showTerms = true }) { Text(stringResource(R.string.terms_read)) }
            ConsentCheckbox(stringResource(R.string.consent_medical), understandsLimitations) {
                understandsLimitations = it
            }
            ConsentCheckbox(stringResource(R.string.consent_terms), agreesToTerms) { agreesToTerms = it }
            Text(
                stringResource(R.string.consent_required_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionButton(
                stringResource(R.string.consent_continue),
                onClick = { if (understandsLimitations && agreesToTerms) onAccept() },
                modifier = Modifier.fillMaxWidth(),
                enabled = understandsLimitations && agreesToTerms,
            )
        }
    }
    if (showPrivacy) PrivacyDialog(onDismiss = { showPrivacy = false })
    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
}

@Composable
private fun ConsentCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
