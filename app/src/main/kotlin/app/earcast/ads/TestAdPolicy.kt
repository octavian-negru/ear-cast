package app.earcast.ads

/** Free-mode banners are automatic; purchase loading never gates access to app features. */
internal data class TestAdPolicy(
    val debugBuild: Boolean,
    val safetyAccepted: Boolean,
    val purchaseLoaded: Boolean,
    val proOwned: Boolean,
    val listening: Boolean,
) {
    val mayRequestAds: Boolean
        get() = debugBuild && safetyAccepted && purchaseLoaded && !proOwned && !listening
}
