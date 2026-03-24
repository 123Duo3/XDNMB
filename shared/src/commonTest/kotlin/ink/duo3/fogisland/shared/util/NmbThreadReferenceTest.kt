package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbThreadReferenceTest {
    @Test
    fun `parse pure thread id`() {
        assertEquals(1234567L, parseNmbThreadIdInput("1234567"))
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
}
