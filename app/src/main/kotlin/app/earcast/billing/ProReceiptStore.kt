package app.earcast.billing

import android.content.Context
import android.util.AtomicFile
import app.earcast.BuildConfig
import com.android.billingclient.api.Purchase
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Receipt rather than a premium boolean; excluded from backup and checked on every launch. */
@Singleton
class ProReceiptStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val file = AtomicFile(File(context.noBackupFilesDir, "pro-receipt.json"))

        fun isValid(purchase: Purchase): Boolean =
            ProPurchaseEvidence(
                products = purchase.products,
                packageName = JSONObject(purchase.originalJson).optString("packageName"),
                completed = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
                token = purchase.purchaseToken,
                signatureValid =
                    PurchaseSignature.verify(
                        BuildConfig.PLAY_PUBLIC_KEY,
                        purchase.originalJson,
                        purchase.signature,
                    ),
            ).grantsAccess(ProBilling.PRODUCT_ID, BuildConfig.APPLICATION_ID)

        fun read(): Boolean =
            runCatching {
                val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
                isValid(Purchase(json.getString("data"), json.getString("signature")))
            }.getOrDefault(false)

        fun save(purchase: Purchase) {
            val bytes =
                JSONObject()
                    .put("data", purchase.originalJson)
                    .put("signature", purchase.signature)
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: java.io.IOException) {
                file.failWrite(output)
                throw error
            }
        }

        fun clear() = file.delete()
    }
