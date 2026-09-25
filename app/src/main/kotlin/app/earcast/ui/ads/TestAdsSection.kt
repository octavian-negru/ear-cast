@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.ads

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.BuildConfig
import app.earcast.R
import app.earcast.ads.TestAdStateModel
import app.earcast.ui.common.CollapsibleNotice

/** Free users receive banners on Home and Settings without an app-level ad toggle. */
@Composable
fun TestAdsSection(showPrivacyInformation: Boolean = false) {
    if (!BuildConfig.DEBUG) return
    val viewModel: TestAdStateModel = hiltViewModel()
    val mayRequestAds by viewModel.mayRequestAds.collectAsStateWithLifecycle()
    if (showPrivacyInformation) {
        CollapsibleNotice(stringResource(R.string.test_ads_title), stringResource(R.string.test_ads_disclosure))
        AdPrivacyLink()
    }
    if (mayRequestAds) TestBanner()
}

@Composable
private fun AdPrivacyLink() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.google_privacy_url)
    TextButton(onClick = { uriHandler.openUri(url) }) {
        Text(stringResource(R.string.google_privacy_label))
    }
}
