package app.earcast.ads

import app.earcast.core.audio.StreamPhase

/** Missing safety acceptance or an active audio session always suppresses banners. */
internal data class TestAdPolicy(
    val enabled: Boolean,
    val safetyAccepted: Boolean,
    val session: StreamPhase,
) {
    val mayRequestAds: Boolean
        get() = enabled && safetyAccepted && (session == StreamPhase.STOPPED || session == StreamPhase.FAILED)
}
