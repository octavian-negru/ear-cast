@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.ui.common.AudiogramCard
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.FeatureRoute
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SectionGroup
import app.earcast.ui.common.SurfaceCard

@Composable
fun ProfileSetupScreen(
    hasProfile: Boolean,
    audiogram: HearingCurve? = null,
    showDinTest: Boolean,
    onRunHearingTest: () -> Unit,
    onManualEntry: () -> Unit,
    onRunDinTest: () -> Unit,
) {
    EarPage {
        BrandBar(stringResource(R.string.identity_profile_label))
        PageHeading(stringResource(R.string.identity_profile_title), stringResource(R.string.identity_profile_detail))
        if (hasProfile) {
            AudiogramCard(audiogram, onManualEntry, stringResource(R.string.profile_editor_action))
            CollapsibleNotice(
                stringResource(R.string.identity_read_chart),
                stringResource(R.string.audiogram_reading_hint),
            )
        } else {
            Text(stringResource(R.string.identity_two_ways), style = MaterialTheme.typography.titleLarge)
        }
        SectionGroup(stringResource(R.string.identity_profile_routes), index = "01") {
            SurfaceCard(Modifier.fillMaxWidth()) {
                FeatureRoute(
                    number = stringResource(R.string.identity_guided_label),
                    title = stringResource(R.string.profile_check_title),
                    detail = stringResource(R.string.profile_check_detail),
                    icon = Icons.Filled.Headphones,
                    onClick = onRunHearingTest,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FeatureRoute(
                    number = stringResource(R.string.identity_manual_label),
                    title = stringResource(R.string.profile_editor_title),
                    detail = stringResource(R.string.profile_editor_detail),
                    icon = Icons.Filled.Edit,
                    onClick = onManualEntry,
                )
            }
        }
        if (showDinTest) {
            FeatureRoute(
                number = stringResource(R.string.home_tools_section),
                title = stringResource(R.string.home_din_test),
                detail = stringResource(R.string.identity_speech_detail),
                icon = Icons.Filled.RecordVoiceOver,
                onClick = onRunDinTest,
            )
        }
        CollapsibleNotice(stringResource(R.string.notice_summary), stringResource(R.string.disclaimer_body))
    }
}
