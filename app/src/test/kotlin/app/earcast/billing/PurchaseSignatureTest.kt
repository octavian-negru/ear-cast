package app.earcast.billing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PurchaseSignatureTest {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKey = Base64.getEncoder().encodeToString(keys.public.encoded)
    private val receipt = """{"productId":"earcast_pro","purchaseState":0}"""

    private fun sign(payload: String): String =
        Signature.getInstance("SHA1withRSA").run {
            initSign(keys.private)
            update(payload.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(sign())
        }

    @Test
    fun `accepts unchanged receipt from configured signing key`() {
        assertTrue(PurchaseSignature.verify(publicKey, receipt, sign(receipt)))
    }

    @Test
    fun `rejects a modified entitlement payload`() {
        assertFalse(PurchaseSignature.verify(publicKey, receipt.replace("0", "2"), sign(receipt)))
    }

    @Test
    fun `rejects receipt signed by another key`() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val otherKey = Base64.getEncoder().encodeToString(other.public.encoded)
        assertFalse(PurchaseSignature.verify(otherKey, receipt, sign(receipt)))
    }

    @Test
    fun `missing configuration and malformed signatures fail closed`() {
        assertFalse(PurchaseSignature.verify("", receipt, sign(receipt)))
        assertFalse(PurchaseSignature.verify(publicKey, receipt, "invalid!"))
        assertFalse(PurchaseSignature.verify("invalid!", receipt, ""))
    }
}
