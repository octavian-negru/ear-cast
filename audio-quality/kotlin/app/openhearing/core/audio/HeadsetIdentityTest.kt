package app.openhearing.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeadsetIdentityTest {
    @Test
    fun `missing address can use one matching product but ambiguous products are rejected`() {
        val output = HeadsetIdentity(1, 7, "", "Headset")
        val first = HeadsetIdentity(2, 7, "", "Headset")
        assertEquals(2, matchingHeadsetInput(listOf(first), output))
        assertNull(matchingHeadsetInput(listOf(first, first.copy(id = 3)), output))
    }

    @Test
    fun `matching product cannot override a conflicting known address`() {
        val output = HeadsetIdentity(1, 7, "one", "Headset")
        val wrong = HeadsetIdentity(2, 7, "two", "Headset")
        assertNull(matchingHeadsetInput(listOf(wrong), output))
        assertEquals(3, matchingHeadsetInput(listOf(wrong, wrong.copy(id = 3, address = "one")), output))
    }
}
