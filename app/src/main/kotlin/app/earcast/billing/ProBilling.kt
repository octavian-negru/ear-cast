package app.earcast.billing

import android.app.Activity
import android.content.Context
import app.earcast.BuildConfig
import app.earcast.R
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ProState(
    val loaded: Boolean = false,
    val owned: Boolean = false,
    val busy: Boolean = false,
    val price: String? = null,
    val message: Int? = null,
)

/** One non-consumable purchase. Play is queried on foregrounding; network failure keeps the signed offline receipt. */
@Singleton
class ProBilling
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val receipts: ProReceiptStore,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val mutex = Mutex()
        private var refreshQueued = false
        private val mutableState = MutableStateFlow(ProState())
        val state = mutableState.asStateFlow()
        private val client =
            BillingClient
                .newBuilder(context)
                .setListener { result, _ ->
                    when (result.responseCode) {
                        BillingClient.BillingResponseCode.OK,
                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                        -> refresh()
                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            mutableState.update { it.copy(message = null) }
                        }
                        else -> mutableState.update { it.copy(message = R.string.pro_store_error) }
                    }
                }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()

        init {
            scope.launch {
                mutex.withLock {
                    val owned = withContext(Dispatchers.IO) { receipts.read() }
                    mutableState.update { it.copy(loaded = true, owned = owned) }
                }
            }
        }

        fun refresh() {
            if (mutableState.value.busy) {
                refreshQueued = true
                return
            }
            transact {
                reconcilePurchases()
                val details = product()
                mutableState.update { it.copy(price = details.oneTimePurchaseOfferDetails?.formattedPrice) }
            }
        }

        fun buy(activity: Activity) =
            transact {
                // Fetch again so the offer and price sent to Play are fresh.
                val details = product()
                val offer = details.oneTimePurchaseOfferDetails ?: error("No buy offer")
                val params =
                    BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(details)
                        .apply { offer.offerToken?.let { setOfferToken(it) } }
                        .build()
                val result =
                    client.launchBillingFlow(
                        activity,
                        BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
                    )
                if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    reconcilePurchases()
                } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                    checkOk(result)
                }
            }

        private fun transact(action: suspend () -> Unit) {
            if (mutableState.value.busy) return
            mutableState.update { it.copy(busy = true, message = null) }
            scope.launch {
                try {
                    mutex.withLock {
                        if (BuildConfig.PLAY_PUBLIC_KEY.isBlank()) {
                            mutableState.update { it.copy(message = R.string.pro_not_configured) }
                        } else {
                            withTimeout(REQUEST_TIMEOUT_MS) {
                                connect()
                                action()
                            }
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    storeFailure()
                } catch (_: IllegalStateException) {
                    storeFailure()
                } catch (_: IllegalArgumentException) {
                    storeFailure()
                } catch (_: java.io.IOException) {
                    storeFailure()
                } catch (_: SecurityException) {
                    storeFailure()
                } finally {
                    mutableState.update { it.copy(busy = false) }
                    if (refreshQueued) {
                        refreshQueued = false
                        refresh()
                    }
                }
            }
        }

        private fun storeFailure() {
            mutableState.update { it.copy(message = R.string.pro_store_error) }
        }

        private suspend fun connect() {
            if (client.isReady) return
            suspendCancellableCoroutine { continuation ->
                client.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            if (!continuation.isActive) return
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                continuation.resume(Unit)
                            } else {
                                continuation.resumeWithException(IllegalStateException("Play unavailable"))
                            }
                        }

                        override fun onBillingServiceDisconnected() = Unit
                    },
                )
            }
        }

        private suspend fun product(): ProductDetails =
            suspendCancellableCoroutine { continuation ->
                val product =
                    QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
                client.queryProductDetailsAsync(params) {
                    result,
                    products,
                    ->
                    if (continuation.isActive) {
                        val details = products.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                        if (result.responseCode == BillingClient.BillingResponseCode.OK && details != null) {
                            continuation.resume(details)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Product unavailable"))
                        }
                    }
                }
            }

        private suspend fun reconcilePurchases() {
            val purchases = queryPurchases()
            val purchase = purchases.firstOrNull { receipts.isValid(it) }
            if (purchase == null) {
                withContext(Dispatchers.IO) { receipts.clear() }
                val pending =
                    purchases.any {
                        PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PENDING
                    }
                mutableState.update {
                    it.copy(owned = false, message = if (pending) R.string.pro_pending else R.string.pro_no_purchase)
                }
            } else {
                if (!purchase.isAcknowledged) acknowledge(purchase.purchaseToken)
                withContext(Dispatchers.IO) { receipts.save(purchase) }
                mutableState.update { it.copy(owned = true, message = null) }
            }
        }

        private suspend fun queryPurchases(): List<Purchase> =
            suspendCancellableCoroutine { continuation ->
                val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                client.queryPurchasesAsync(params) {
                    result,
                    purchases,
                    ->
                    if (continuation.isActive) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(purchases)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Purchase query failed"))
                        }
                    }
                }
            }

        private suspend fun acknowledge(token: String): Unit =
            suspendCancellableCoroutine { continuation ->
                val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
                client.acknowledgePurchase(params) { result ->
                    if (continuation.isActive) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Acknowledgment failed"))
                        }
                    }
                }
            }

        private fun checkOk(result: BillingResult) {
            check(result.responseCode == BillingClient.BillingResponseCode.OK) { "Play operation failed" }
        }

        companion object {
            const val PRODUCT_ID = "earcast_pro"
            private const val REQUEST_TIMEOUT_MS = 20_000L
        }
    }
