package app.earcast.assist

import app.earcast.core.audio.speech.EnhancementEngine
import app.earcast.core.audio.speech.EnhancementOptions
import app.earcast.core.audio.speech.NoiseStrength
import app.earcast.core.audio.speech.VoiceBoost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PremiumAudioPolicyTest {
    @Test
    fun freeSessionFallsBackFromSavedPremiumOptions() {
        val saved =
            EnhancementOptions(
                speechEngine = EnhancementEngine.DPDFNET,
                quietSpeech = VoiceBoost.STRONG,
                noiseReduction = NoiseStrength.STRONG,
            )

        val effective = saved.forEntitlement(proOwned = false)

        assertEquals(EnhancementEngine.RNNOISE, effective.speechEngine)
        assertEquals(VoiceBoost.GENTLE, effective.quietSpeech)
        assertEquals(NoiseStrength.STRONG, effective.noiseReduction)
        assertFalse(effective.requiresPro())
        assertTrue(saved.requiresPro())
        assertEquals(saved, saved.forEntitlement(proOwned = true))
    }

    @Test
    fun freeAlternativesRemainAvailable() {
        val classical = EnhancementOptions(speechEngine = EnhancementEngine.SPEEX)
        assertEquals(classical, classical.forEntitlement(proOwned = false))
    }
}
