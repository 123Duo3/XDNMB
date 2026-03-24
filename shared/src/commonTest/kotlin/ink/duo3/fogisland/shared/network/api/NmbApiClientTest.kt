package ink.duo3.fogisland.shared.network.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbApiClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `treat quoted error message as business error for non string responses`() {
        assertEquals(
            expected = "该串不存在",
            actual = detectNmbBusinessErrorMessage(
                json = json,
                bodyText = "\"该串不存在\"",
                expectsRawString = false
            )
        )
    }

    @Test
    fun `allow quoted string for string responses`() {
        assertNull(
            detectNmbBusinessErrorMessage(
                json = json,
                bodyText = "\"操作成功\"",
                expectsRawString = true
            )
        )
    }

    @Test
    fun `ignore object responses`() {
        assertNull(
            detectNmbBusinessErrorMessage(
                json = json,
                bodyText = """{"success":true}""",
                expectsRawString = false
            )
        )
    }
}
