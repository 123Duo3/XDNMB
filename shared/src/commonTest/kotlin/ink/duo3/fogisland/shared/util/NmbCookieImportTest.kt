package ink.duo3.fogisland.shared.util

import ink.duo3.fogisland.shared.model.CookieProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbCookieImportTest {
    @Test
    fun parseQrPayload() {
        val payload = parseNmbCookieImportPayload(
            """{"cookie":"test_cookie_value_123","name":"sampleUser"}"""
        )

        assertEquals("test_cookie_value_123", payload.cookieValue)
        assertEquals("sampleUser", payload.accountName)
    }

    @Test
    fun parsePlainCookiePayload() {
        val payload = parseNmbCookieImportPayload("plain-cookie")

        assertEquals("plain-cookie", payload.cookieValue)
        assertNull(payload.accountName)
    }

    @Test
    fun cookieDisplayNameUsesAccountName() {
        val profile = CookieProfile(
            id = "1",
            cookieValue = "plain-cookie",
            accountName = "sampleUser",
            remark = "主饼干"
        )

        assertEquals("sampleUser", profile.displayName)
    }
}
