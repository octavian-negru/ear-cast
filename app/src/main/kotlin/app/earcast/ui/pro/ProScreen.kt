@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.pro

import androidx.compose.foundation.layout.fillMaxWidth
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
import app.earcast.ui.common.PageHeading
import app.earcast.ui.common.SurfaceCard

@Composable
fun ProScreen(
    state: ProState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onContinue: () -> Unit,
) {
    EarPage {
        BrandBar(stringResource(R.string.pro_label))
        PageHeading(stringResource(R.string.pro_title), stringResource(R.string.pro_description))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pro_free_features), style = MaterialTheme.typography.bodyMedium)
        }
        ActionButton(
            label = stringResource(if (state.owned) R.string.back else R.string.pro_continue_free),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            style = ActionStyle.SECONDARY,
        )
        Text(
            stringResource(if (state.owned) R.string.pro_owned else R.string.pro_offline),
            style = MaterialTheme.typography.bodyMedium,
        )
        ActionButton(
            label =
                state.price?.let { stringResource(R.string.pro_buy, it) }
                    ?: stringResource(R.string.pro_price_unavailable),
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.owned && state.price != null && !state.busy,
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
