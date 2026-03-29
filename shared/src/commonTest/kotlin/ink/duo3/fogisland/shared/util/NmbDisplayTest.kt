package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NmbDisplayTest {

    @Test
    fun titleBlankIsNormalizedAway() {
        assertNull(normalizeNmbStoredTitle("   "))
    }

    @Test
    fun titleTrimsNormalContent() {
        assertEquals("标题", normalizeNmbStoredTitle("  标题  "))
    }

    @Test
    fun titlePlaceholderIsNormalizedAway() {
        assertNull(normalizeNmbStoredTitle("无标题"))
    }

    @Test
    fun namePlaceholderIsNormalizedAway() {
        assertNull(normalizeNmbStoredName("无名氏"))
    }

    @Test
    fun nameBlankIsNormalizedAway() {
        assertNull(normalizeNmbStoredName("   "))
    }

    @Test
    fun nameTrimsNormalContent() {
        assertEquals("张三", normalizeNmbStoredName("  张三  "))
    }
}
