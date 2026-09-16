package app.earcast.core.audio.dsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaEffectSessionTest {
    private val bands = listOf(MediaBand(1_000.0, 2_000.0, -3.0, 0.0))
    private val configuration = MediaSoundConfig(bands)

    @Test
    fun `nested app playback suspends synchronously and restores only the latest settings`() {
        var active = 0
        val applied = mutableListOf<MediaSoundConfig>()
        val session =
            MediaEffectSession {
                applied += it
                active++
                effect(onClose = { active-- })
            }
        assertTrue(session.apply(configuration))
        assertTrue(session.apply(configuration))
        assertEquals(1, applied.size, "unchanged settings must not recreate a live effect")
        val assist = session.bypass()
        assertEquals(0, active, "effect must be gone before audio starts")
        val demo = session.bypass()
        val changed = configuration.copy(bands = listOf(bands.single().copy(leftGainDb = -6.0)), boostDb = 15f)
        assertTrue(session.apply(changed))
        assist.close()
        assist.close()
        assertEquals(0, active, "another player still owns a bypass")
        demo.close()
        assertEquals(1, active)
        assertEquals(listOf(configuration, changed), applied)
        session.release()
        assertEquals(0, active)
    }

    @Test
    fun `disabling EQ while app audio plays prevents restoration`() {
        var created = 0
        val session =
            MediaEffectSession {
                created++
                effect()
            }
        val playback = session.bypass()
        assertTrue(session.apply(configuration))
        assertEquals(0, created)
        session.release()
        playback.close()
        assertFalse(session.isActive)
        assertEquals(0, created)
    }

    @Test
    fun `a failed effect can be retried without leaking a playback lease`() {
        var fail = true
        val session =
            MediaEffectSession {
                check(!fail)
                effect()
            }
        assertFalse(session.apply(configuration))
        val playback = session.bypass()
        playback.close()
        assertFalse(session.isActive)
        fail = false
        assertTrue(session.apply(configuration))
        assertTrue(session.isActive)
    }

    @Test
    fun `media volume updates without recreating the effect and survives playback bypass`() {
        val started = mutableListOf<Float>()
        val updated = mutableListOf<Float>()
        val session =
            MediaEffectSession {
                started += it.boostDb
                effect(onBoost = { db -> updated += db })
            }
        assertTrue(session.apply(configuration))
        assertTrue(session.apply(configuration.copy(boostDb = 15f)))
        assertEquals(listOf(6f), started)
        assertEquals(listOf(15f), updated)
        val playback = session.bypass()
        assertTrue(session.apply(configuration.copy(boostDb = 3f)))
        assertEquals(listOf(15f), updated, "no updates to a released effect")
        playback.close()
        assertEquals(listOf(6f, 3f), started)
    }

    @Test
    fun `failed live update releases the effect and allows retry`() {
        var closed = 0
        val session =
            MediaEffectSession {
                effect(onBoost = { error("effect lost control") }, onClose = { closed++ })
            }
        assertTrue(session.apply(configuration))
        val changed = configuration.copy(boostDb = 7f)
        assertFalse(session.apply(changed))
        assertFalse(session.isActive)
        assertEquals(1, closed)
        assertTrue(session.apply(changed))
        assertTrue(session.isActive)
    }

    private fun effect(
        onBoost: (Float) -> Unit = {},
        onClose: () -> Unit = {},
    ): MediaEffectHandle =
        object : MediaEffectHandle {
            override fun setBoostDb(db: Float) = onBoost(db)

            override fun close() = onClose()
        }
}
