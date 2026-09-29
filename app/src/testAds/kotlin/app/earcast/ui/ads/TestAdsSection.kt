@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.ads

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.ads.TestAdStateModel

@Composable
fun TestAdsSection() {
    val viewModel: TestAdStateModel = hiltViewModel()
    val mayRequestAds by viewModel.mayRequestAds.collectAsStateWithLifecycle()
    if (mayRequestAds) {
        Text(stringResource(R.string.test_ads_disclosure))
        TestBanner()
    }
}
