package app.openhearing.core.audio.diagnostics

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SessionDiagnosticsTest {
    @TempDir lateinit var temporary: File

    @Test
    fun `close drains all published blocks and writes synchronized float channels`() {
        val done = CountDownLatch(1)
        var failure: String? = null
        val directory = File(temporary, "recording")
        val recorder = SessionDiagnostics(
            directory, 16_000, 64, mapOf("notes" to "quotes \" and newline\n"),
        ) { _, error ->
            failure = error
            done.countDown()
        }
        repeat(64) { block ->
            recorder.input(FloatArray(128) { block / 100f })
            recorder.enhanced(FloatArray(128) { block / 200f })
            recorder.output(FloatArray(128) { if (it % 2 == 0) 0.3f else -0.3f }, 1000)
        }
        recorder.close()
        recorder.close()
        assertTrue(done.await(10, TimeUnit.SECONDS), "Writer did not finish")
        assertNull(failure)
        val bytes = File(directory, "stages.wav").readBytes()
        assertEquals(56 + 64 * 64 * 4 * 4, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(3, buffer.getShort(20).toInt())
        assertEquals(4, buffer.getShort(22).toInt())
        assertEquals(16_000, buffer.getInt(24))
        buffer.position(56)
        repeat(64) { block ->
            repeat(64) {
                assertEquals(block / 100f, buffer.float)
                assertEquals(block / 200f, buffer.float)
                assertEquals(0.3f, buffer.float)
                assertEquals(-0.3f, buffer.float)
            }
        }
        assertEquals(65, File(directory, "blocks.csv").readLines().size)
        assertTrue(File(directory, "metadata.json").readText().contains("session_ended"))
    }
}
