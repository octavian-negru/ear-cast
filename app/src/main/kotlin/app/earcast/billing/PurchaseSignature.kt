package app.earcast.billing

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Play's signed purchase payload; no private signing key belongs in the app. */
object PurchaseSignature {
    fun verify(
        publicKey: String,
        payload: String,
        signature: String,
    ): Boolean =
        runCatching {
            val key =
                KeyFactory.getInstance("RSA").generatePublic(
                    X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)),
                )
            Signature.getInstance("SHA1withRSA").run {
                initVerify(key)
                update(payload.toByteArray(Charsets.UTF_8))
                verify(Base64.getDecoder().decode(signature))
            }
        }.getOrDefault(false)
}
