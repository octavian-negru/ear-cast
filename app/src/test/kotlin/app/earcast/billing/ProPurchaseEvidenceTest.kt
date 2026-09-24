package app.earcast.billing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProPurchaseEvidenceTest {
    private val valid = ProPurchaseEvidence(listOf("earcast_pro"), "app.earcast", true, "token", true)

    private fun ProPurchaseEvidence.allowed() = grantsAccess("earcast_pro", "app.earcast")

    @Test
    fun `completed authenticated Pro purchase grants access`() {
        assertTrue(valid.allowed())
    }

    @Test
    fun `pending payment never grants access even with a valid signature`() {
        assertFalse(valid.copy(completed = false).allowed())
    }

    @Test
    fun `another app or another product cannot unlock Pro`() {
        assertFalse(valid.copy(packageName = "another.app").allowed())
        assertFalse(valid.copy(products = listOf("another_product")).allowed())
        assertFalse(valid.copy(products = emptyList()).allowed())
    }

    @Test
    fun `missing token or invalid signature cannot unlock Pro`() {
        assertFalse(valid.copy(token = " ").allowed())
        assertFalse(valid.copy(signatureValid = false).allowed())
    }
}
