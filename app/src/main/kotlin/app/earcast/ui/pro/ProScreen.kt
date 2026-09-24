@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.pro

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.earcast.R
import app.earcast.billing.ProState
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionStyle
import app.earcast.ui.common.BrandBar
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.EarPage
import app.earcast.ui.common.JourneyStep
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SurfaceCard

@Composable
fun ProScreen(
    state: ProState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    EarPage(modifier = Modifier.safeDrawingPadding()) {
        BrandBar(stringResource(R.string.pro_label))
        PageHeading(stringResource(R.string.pro_title), stringResource(R.string.pro_description))
        SurfaceCard(Modifier.fillMaxWidth()) {
            JourneyStep("01", stringResource(R.string.pro_profile_title), stringResource(R.string.pro_profile_detail))
            JourneyStep("02", stringResource(R.string.pro_listen_title), stringResource(R.string.pro_listen_detail))
            JourneyStep("03", stringResource(R.string.pro_media_title), stringResource(R.string.pro_media_detail))
        }
        Text(stringResource(R.string.pro_offline), style = MaterialTheme.typography.bodyMedium)
        ActionButton(
            label =
                state.price?.let { stringResource(R.string.pro_buy, it) }
                    ?: stringResource(R.string.pro_price_unavailable),
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.price != null && !state.busy,
        )
        ActionButton(
            label = stringResource(if (state.busy) R.string.pro_checking else R.string.pro_restore),
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            style = ActionStyle.SECONDARY,
            enabled = !state.busy,
        )
        state.message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
        Text(stringResource(R.string.pro_account), style = MaterialTheme.typography.bodySmall)
        CollapsibleNotice(stringResource(R.string.disclaimer_title), stringResource(R.string.disclaimer_body))
    }
}
