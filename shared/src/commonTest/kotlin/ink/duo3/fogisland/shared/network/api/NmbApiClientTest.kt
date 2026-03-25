package ink.duo3.fogisland.shared.network.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `extract html form error message`() {
        assertEquals(
            expected = "内容不能为空",
            actual = extractHtmlFormErrorMessage(
                """<html><body><p class="error">内容不能为空</p></body></html>"""
            )
        )
    }

    @Test
    fun `extract html form success message`() {
        assertEquals(
            expected = "发串成功",
            actual = extractHtmlFormSuccessMessage(
                """<html><body><p class="success">发串成功</p></body></html>"""
            )
        )
    }

    @Test
    fun `extract html reply success message`() {
        assertEquals(
            expected = "回复成功",
            actual = extractHtmlFormSuccessMessage(
                """<html><body><p class="success">回复成功</p></body></html>"""
            )
        )
    }

    @Test
    fun `normalize comparable post content trims outer blank lines and edge spaces`() {
        assertEquals(
            expected = "leading\nline  \n\n\tinner",
            actual = normalizeComparablePostContent(
                "\r\n  leading\r\nline  \r\n\r\n\tinner\r\n"
            )
        )
    }

    @Test
    fun `normalize comparable post content trims first line leading and last line trailing spaces`() {
        assertEquals(
            expected = "outer-leading\nmiddle\nouter-trailing",
            actual = normalizeComparablePostContent(
                "  outer-leading\nmiddle\nouter-trailing  "
            )
        )
    }

    @Test
    fun `image only content accepts server default share image marker`() {
        assertTrue(
            comparablePostedContentCandidates(
                requestContent = "",
                hasImage = true
            ).contains(normalizeComparablePostContent("分享图片"))
        )
    }

    @Test
    fun `resolve canonical posted content prefers confirmed server content`() {
        assertEquals(
            expected = "server\ncontent",
            actual = resolveCanonicalPostedContent(
                confirmedPost = ink.duo3.fogisland.shared.network.model.LastPostDto(
                    id = 1L,
                    content = "server<br />\ncontent"
                ),
                requestContent = "request"
            )
        )
    }

    @Test
    fun `resolve canonical posted content falls back to request content`() {
        assertEquals(
            expected = "request",
            actual = resolveCanonicalPostedContent(
                confirmedPost = null,
                requestContent = "request"
            )
        )
    }

    @Test
    fun `extract thread id from redirected thread url`() {
        assertEquals(
            expected = 52752005L,
            actual = extractThreadIdFromUrl("https://www.nmbxd1.com/t/52752005")
        )
    }

    @Test
    fun `allow confirmation failure after primary success is already known`() {
        assertNull(
            resolveOptionalPostConfirmation(
                primarySuccess = true,
                confirmationResult = Result.failure(IllegalStateException("boom"))
            )
        )
    }

    @Test
    fun `rethrow confirmation failure when primary success is unknown`() {
        val throwable = assertFailsWith<IllegalStateException> {
            resolveOptionalPostConfirmation(
                primarySuccess = false,
                confirmationResult = Result.failure(IllegalStateException("boom"))
            )
        }

        assertEquals("boom", throwable.message)
    }
}
