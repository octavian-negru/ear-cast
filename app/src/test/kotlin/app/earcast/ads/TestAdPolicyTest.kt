package app.earcast.ads

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestAdPolicyTest {
    private val freeUser = TestAdPolicy(true, true, true, false, false)

    @Test
    fun `free users receive banners automatically after safety onboarding`() {
        assertTrue(freeUser.mayRequestAds)
        assertFalse(freeUser.copy(safetyAccepted = false).mayRequestAds)
    }

    @Test
    fun `owners and unresolved purchases never receive an ad request`() {
        assertFalse(freeUser.copy(proOwned = true).mayRequestAds)
        assertFalse(freeUser.copy(purchaseLoaded = false).mayRequestAds)
    }

    @Test
    fun `listening suppresses banners and stopping restores them for free users`() {
        val listening = freeUser.copy(listening = true)
        assertFalse(listening.mayRequestAds)
        assertTrue(listening.copy(listening = false).mayRequestAds)
    }

    @Test
    fun `losing Pro returns to automatic free mode banners`() {
        val pro = freeUser.copy(proOwned = true)
        assertFalse(pro.mayRequestAds)
        assertTrue(pro.copy(proOwned = false).mayRequestAds)
    }

    @Test
    fun `release builds never request test inventory`() {
        assertFalse(freeUser.copy(debugBuild = false).mayRequestAds)
    }
}
