package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NmbRichTextTest {
    @Test
    fun `parse quoted thread reference with color`() {
        val richText = parseNmbRichText("<font color=\"#789922\">&gt;&gt;No.123456</font><br>正文")

        assertEquals(">>No.123456\n正文", richText.plainText)
        assertEquals(3, richText.segments.size)
        assertEquals(">>No.123456", richText.segments[0].text)
        assertEquals("#789922", richText.segments[0].color)
        assertEquals(123456L, richText.segments[0].threadId)
        assertEquals("\n", richText.segments[1].text)
        assertEquals("正文", richText.segments[2].text)
    }

    @Test
    fun `parse link and inline color span`() {
        val richText = parseNmbRichText(
            "<span style=\" color: green \">公告</span> <a href=\"https://app.nmbxd.com\">客户端下载</a>"
        )

        assertEquals("公告 客户端下载", richText.plainText)
        assertEquals("green", richText.segments[0].color)
        assertEquals("公告", richText.segments[0].text)
        assertEquals("https://app.nmbxd.com", richText.segments[2].href)
        assertEquals("客户端下载", richText.segments[2].text)
    }

    @Test
    fun `parse roll rich text`() {
        val richText = parseNmbRichText("午饭是<b>10</b><small>[5,20]</small>元")

        assertEquals("午饭是10[5,20]元", richText.plainText)
        assertEquals("10", richText.segments[1].text)
        assertTrue(richText.segments[1].isBold)
        assertEquals("[5,20]", richText.segments[2].text)
        assertTrue(richText.segments[2].isSmall)
    }

    @Test
    fun `parse hidden text and raw url`() {
        val richText = parseNmbRichText("前文[h]隐藏内容[/h]后文<br>https://liuyan.people.com.cn/")

        assertEquals("前文隐藏内容后文\nhttps://liuyan.people.com.cn/", richText.plainText)
        val hiddenSegment = richText.segments.firstOrNull { it.isHidden }
        assertNotNull(hiddenSegment)
        assertEquals("隐藏内容", hiddenSegment.text)
        assertEquals(
            "https://liuyan.people.com.cn/",
            richText.segments.last().href
        )
    }

    @Test
    fun `parse italic underline and strikethrough`() {
        val richText = parseNmbRichText("<i>斜体</i><u>下划线</u><del>删除线</del>")

        assertEquals("斜体下划线删除线", richText.plainText)
        assertTrue(richText.segments[0].isItalic)
        assertTrue(richText.segments[1].isUnderline)
        assertTrue(richText.segments[2].isStrikethrough)
    }

    @Test
    fun `plain text stays plain when no html formatting exists`() {
        val richText = parseNmbRichText("普通正文(ﾟ∀ﾟ)")

        assertEquals("普通正文(ﾟ∀ﾟ)", richText.plainText)
        assertEquals(1, richText.segments.size)
        assertFalse(richText.segments[0].isBold)
        assertFalse(richText.segments[0].isSmall)
        assertNull(richText.segments[0].href)
        assertNull(richText.segments[0].threadId)
    }

    @Test
    fun `rich text render hint only marks actual rich markup`() {
        assertFalse(shouldRenderNmbRichText("普通正文"))
        assertTrue(shouldRenderNmbRichText("<font color=\"#789922\">引用</font>"))
        assertTrue(shouldRenderNmbRichText("[h]隐藏内容[/h]"))
    }
}
