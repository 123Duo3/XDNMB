package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbThreadReferenceTest {
    @Test
    fun `parse post references with arrow and optional no prefix`() {
        assertEquals(1234567L, parseNmbPostReference(">>No.1234567"))
        assertEquals(1234567L, parseNmbPostReference(">> 1234567"))
        assertEquals(1234567L, parseNmbPostReference("No.1234567"))
    }

    @Test
    fun `reject short bare no references`() {
        assertNull(parseNmbPostReference("No.1234"))
    }

    @Test
    fun `parse pure thread id`() {
        assertEquals(1234567L, parseNmbThreadIdInput("1234567"))
    }

    @Test
    fun `parse quoted eight digit thread id`() {
        assertEquals(12345678L, parseNmbQuotedThreadIdInput(">>12345678"))
        assertEquals(12345678L, parseNmbQuotedThreadIdInput(">12345678"))
        assertEquals(12345678L, parseNmbThreadIdInput(">>12345678"))
        assertEquals(12345678L, parseNmbThreadIdInput(">12345678"))
        assertNull(parseNmbQuotedThreadIdInput(">>1234567"))
    }

    @Test
    fun `parse no prefix thread id`() {
        assertEquals(1234567L, parseNmbThreadIdInput("No.1234567"))
        assertEquals(1234567L, parseNmbThreadIdInput(">>No.1234567"))
        assertEquals(1234567L, parseNmbThreadIdInput(">> no. 1234567"))
    }

    @Test
    fun `parse thread url`() {
        assertEquals(
            1234567L,
            parseNmbThreadIdInput("https://www.nmbxd1.com/t/1234567")
        )
        assertEquals(
            1234567L,
            parseNmbThreadIdInput("https://www.nmbxd1.com/t/1234567?page=2")
        )
        assertEquals(
            1234567L,
            parseNmbThreadIdInput("https://www.nmbxd.com/t/1234567?r=7654321")
        )
    }

    @Test
    fun `reject unrelated text`() {
        assertNull(parseNmbThreadIdInput("综合线"))
        assertNull(parseNmbThreadIdInput("https://www.nmbxd1.com/f/4"))
    }

    @Test
    fun `resolve thread url as internal link target`() {
        assertEquals(
            NmbLinkTarget.Thread(
                threadId = 1234567L,
                targetPage = 2
            ),
            resolveNmbUrlLinkTarget("https://www.nmbxd1.com/t/1234567?page=2")
        )
    }

    @Test
    fun `resolve thread reply url as internal link target`() {
        assertEquals(
            NmbLinkTarget.Thread(
                threadId = 1234567L,
                targetPostId = 7654321L
            ),
            resolveNmbUrlLinkTarget("https://www.nmbxd.com/t/1234567?r=7654321")
        )
    }

    @Test
    fun `resolve external url as external link target`() {
        assertEquals(
            NmbLinkTarget.ExternalUrl("https://app.nmbxd.com"),
            resolveNmbUrlLinkTarget("https://app.nmbxd.com")
        )
    }
}
