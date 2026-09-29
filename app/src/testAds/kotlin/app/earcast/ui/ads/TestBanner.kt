@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.ads

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.earcast.R
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// Google's dedicated adaptive test banner; no production ID can be supplied to this build.
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"

private object TestAdSdk {
    val ready = MutableStateFlow(false)
    private val requested = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (!requested.compareAndSet(false, true)) return
        val application = context.applicationContext
        scope.launch {
            try {
                // Google's limited-ads setting: do not infer cookie consent from free-mode use.
                val stored =
                    application
                        .getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("gad_has_consent_for_cookies", 0)
                        .commit()
                if (!stored) {
                    requested.set(false)
                    return@launch
                }
                MobileAds.initialize(application) {
                    MobileAds.setAppMuted(true)
                    ready.value = true
                }
            } catch (error: IllegalStateException) {
                requested.set(false)
                Log.w("EarCastAds", "Test ad initialization unavailable", error)
            }
        }
    }
}

/** Only compiled into explicitly enabled debug builds. */
@Composable
internal fun TestBanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    if (!lifecycle.isAtLeast(Lifecycle.State.RESUMED)) return
    val ready by TestAdSdk.ready.collectAsState()
    LaunchedEffect(context) { TestAdSdk.initialize(context) }
    if (!ready) return
    var retry by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.test_ads_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            val width = maxWidth.value.toInt()
            if (width > 0) {
                key(width, retry) { AdaptiveTestBanner(width, onRetry = { retry++ }) }
            }
        }
    }
}

@Composable
private fun AdaptiveTestBanner(
    width: Int,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    val size = remember(context, width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width) }
    val ad =
        remember(context, size) {
            AdView(context).apply {
                adUnitId = TEST_BANNER_ID
                setAdSize(size)
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("EarCastAds", "Test banner loaded")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("EarCastAds", "Test banner failed: ${error.code} ${error.message}")
                            failed = true
                        }
                    }
            }
        }
    if (failed) {
        TextButton(onClick = onRetry) { Text(stringResource(R.string.test_ads_retry)) }
    } else {
        AndroidView(factory = { ad }, modifier = Modifier.fillMaxWidth().height(size.height.dp))
    }
    DisposableEffect(ad) {
        val extras = Bundle().apply { putString("npa", "1") }
        ad.loadAd(AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build())
        onDispose { ad.destroy() }
    }
}
