package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbDisplayTest {

    @Test
    fun titleReturnsNullWhenBlank() {
        assertNull(resolveNmbDisplayTitle("   "))
    }

    @Test
    fun authorPrefersUserHashWhenPresent() {
        assertEquals("Abc123", resolveNmbDisplayAuthor("Abc123", "无名氏"))
    }

    @Test
    fun titlePlaceholderIsNormalizedAway() {
        assertEquals("", normalizeNmbStoredTitle("无标题"))
        assertNull(resolveNmbDisplayTitle("无标题"))
    }

    @Test
    fun namePlaceholderIsNormalizedAway() {
        assertEquals("", normalizeNmbStoredName("无名氏"))
        assertNull(resolveNmbDisplayAuthor("", "无名氏"))
    }

    @Test
    fun authorFallsBackToNameWhenUserHashBlank() {
        assertEquals("张三", resolveNmbDisplayAuthor("   ", "张三"))
    }

    @Test
    fun authorReturnsNullWhenBothFieldsBlank() {
        assertNull(resolveNmbDisplayAuthor("", "   "))
    }
}
