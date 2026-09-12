package app.openhearing.core.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AssistPcmBufferTest {
    @Test
    fun `phone PCM reaches both independent ear processors and retains signed extremes`() {
        val buffers = AssistPcmBuffer(AudioFormat(48_000, 2, 3), 2)
        shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE).copyInto(buffers.input)
        buffers.process { block ->
            assertArrayEquals(floatArrayOf(-1f, -1f, 0f, 0f, 32767f / 32768, 32767f / 32768), block)
            for (i in block.indices step 2) block[i + 1] = 0f
        }
        assertArrayEquals(shortArrayOf(Short.MIN_VALUE, 0, 0, 0, Short.MAX_VALUE, 0), buffers.output)
    }

    @Test
    fun `SCO mixes limited ear outputs before PCM conversion and reuses its buffers`() {
        val buffers = AssistPcmBuffer(AudioFormat(16_000, 2, 2), 1)
        val output = buffers.output
        buffers.process { block -> floatArrayOf(0.4f, 0.2f, -0.4f, -0.2f).copyInto(block) }
        assertEquals(9830, buffers.output[0].toInt())
        assertEquals(-9830, buffers.output[1].toInt())
        assertTrue(buffers.output.all { abs(it / 32768f) <= 0.3f })
        buffers.input.fill(0)
        buffers.process { }
        assertTrue(output === buffers.output)
        assertArrayEquals(shortArrayOf(0, 0), buffers.output)
    }
}
