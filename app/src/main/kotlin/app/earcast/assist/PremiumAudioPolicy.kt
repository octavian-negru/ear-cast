package app.earcast.assist

import app.earcast.core.audio.speech.EnhancementEngine
import app.earcast.core.audio.speech.EnhancementOptions
import app.earcast.core.audio.speech.VoiceBoost

/** DPDFNet8 and strong quiet-speech boosting are the Pro amplification options. */
fun EnhancementOptions.requiresPro(): Boolean {
    val advancedEngine = speechEngine == EnhancementEngine.DPDFNET
    return advancedEngine || quietSpeech == VoiceBoost.STRONG
}

/** Apply free defaults without altering saved Pro preferences. */
fun EnhancementOptions.forEntitlement(proOwned: Boolean): EnhancementOptions =
    if (proOwned) {
        this
    } else {
        copy(
            speechEngine = if (speechEngine == EnhancementEngine.DPDFNET) EnhancementEngine.RNNOISE else speechEngine,
            quietSpeech = if (quietSpeech == VoiceBoost.STRONG) VoiceBoost.GENTLE else quietSpeech,
        )
    }
