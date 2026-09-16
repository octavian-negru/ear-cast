package app.earcast.di

import app.earcast.audiogram.ProfileFitting
import app.earcast.audiogram.halfGainFitting
import app.earcast.core.audio.PreviewPlayer
import app.earcast.core.audio.TestSignalGenerator
import app.earcast.core.audio.TestSignalPlayer
import app.earcast.mediaeq.MediaSoundController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the audio + fitting collaborators used by the hearing-test screen. */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackBindings {
    @Provides
    @Singleton
    fun toneGenerator(): TestSignalGenerator = TestSignalGenerator()

    // Not a singleton: each consumer gets its own AudioTrack-backed player and is
    // responsible for releasing it.
    @Provides
    fun tonePlayer(mediaEq: MediaSoundController): TestSignalPlayer =
        TestSignalPlayer(
            bypassEffects = mediaEq::bypassForPlayback,
        )

    // Same lifecycle contract as TestSignalPlayer: per-consumer, released by its owner.
    @Provides
    fun abPlayer(mediaEq: MediaSoundController): PreviewPlayer =
        PreviewPlayer(
            bypassEffects = mediaEq::bypassForPlayback,
        )

    @Provides
    @Singleton
    fun fittingStrategy(): ProfileFitting = halfGainFitting()
}
