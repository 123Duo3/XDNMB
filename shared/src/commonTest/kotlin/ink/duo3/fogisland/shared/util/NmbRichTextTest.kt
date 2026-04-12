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
        assertEquals(NmbLinkTarget.PostReference(123456L), richText.segments[0].linkTarget)
        assertEquals("\n", richText.segments[1].text)
        assertEquals("正文", richText.segments[2].text)
    }

    @Test
    fun `parse quoted thread reference without no prefix`() {
        val richText = parseNmbRichText("前文 &gt;&gt;123456 后文")

        val referenceSegment = richText.segments.firstOrNull {
            it.linkTarget == NmbLinkTarget.PostReference(123456L)
        }
        assertNotNull(referenceSegment)
        assertEquals(">>123456", referenceSegment.text.trim())
    }

    @Test
    fun `parse eight digit arrow thread id as thread target`() {
        val richText = parseNmbRichText("前文 &gt;&gt;12345678 和 &gt;87654321")

        val threadTargets = richText.segments.mapNotNull { it.linkTarget as? NmbLinkTarget.Thread }
        assertEquals(
            listOf(
                NmbLinkTarget.Thread(threadId = 12345678L),
                NmbLinkTarget.Thread(threadId = 87654321L)
            ),
            threadTargets
        )
    }

    @Test
    fun `do not parse eight digit arrow thread id inside small tag`() {
        val richText = parseNmbRichText("<small>&gt;&gt;12345678</small> 外边 &gt;87654321")

        assertEquals(">>12345678 外边 >87654321", richText.plainText)
        assertNull(richText.segments.first().linkTarget)
        assertTrue(richText.segments.first().isSmall)
        assertEquals(
            NmbLinkTarget.Thread(threadId = 87654321L),
            richText.segments.last().linkTarget
        )
    }

    @Test
    fun `parse no prefix reference only when it is long enough`() {
        val longReference = parseNmbRichText("引用 No.12345")
        val shortReference = parseNmbRichText("引用 No.1234")

        assertEquals(
            NmbLinkTarget.PostReference(12345L),
            longReference.segments.last().linkTarget
        )
        assertNull(shortReference.segments.last().linkTarget)
    }

    @Test
    fun `parse link and inline color span`() {
        val richText = parseNmbRichText(
            "<span style=\" color: green \">公告</span> <a href=\"https://app.nmbxd.com\">客户端下载</a>"
        )

        assertEquals("公告 客户端下载", richText.plainText)
        assertEquals("green", richText.segments[0].color)
        assertEquals("公告", richText.segments[0].text)
        val linkSegment = richText.segments.firstOrNull {
            it.linkTarget == NmbLinkTarget.ExternalUrl("https://app.nmbxd.com")
        }
        assertNotNull(linkSegment)
        assertEquals("客户端下载", linkSegment.text.trim())
    }

    @Test
    fun `normalize named html colors`() {
        val richText = parseNmbRichText("<font color=\"DeepSkyBlue\">蓝色文字</font>")

        assertEquals("蓝色文字", richText.plainText)
        assertEquals("#00BFFF", richText.segments.single().color)
    }

    @Test
    fun `parse thread url link as internal thread target`() {
        val richText = parseNmbRichText("<a href=\"https://www.nmbxd1.com/t/1234567?page=2\">串链接</a>")

        assertEquals("串链接", richText.plainText)
        assertEquals(
            NmbLinkTarget.Thread(
                threadId = 1234567L,
                targetPage = 2
            ),
            richText.segments.single().linkTarget
        )
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
            NmbLinkTarget.ExternalUrl("https://liuyan.people.com.cn/"),
            richText.segments.last().linkTarget
        )
    }

    @Test
    fun `adjacent hidden texts stay separate segments`() {
        val richText = parseNmbRichText("[h]您好[/h][h]我好[/h][h]您吃了吗？[/h]")

        assertEquals("您好我好您吃了吗？", richText.plainText)
        val hiddenSegments = richText.segments.filter { it.isHidden }
        assertEquals(3, hiddenSegments.size)
        assertEquals("您好", hiddenSegments[0].text)
        assertEquals("我好", hiddenSegments[1].text)
        assertEquals("您吃了吗？", hiddenSegments[2].text)
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
    fun `parse html list markers`() {
        val unordered = parseNmbRichText("<ul><li>第一条</li><li>第二条</li></ul>")
        val ordered = parseNmbRichText("<ol><li>第一条</li><li>第二条</li></ol>")

        assertEquals("• 第一条\n• 第二条\n", unordered.plainText)
        assertEquals("1. 第一条\n2. 第二条\n", ordered.plainText)
    }

    @Test
    fun `plain text stays plain when no html formatting exists`() {
        val richText = parseNmbRichText("普通正文(ﾟ∀ﾟ)")

        assertEquals("普通正文(ﾟ∀ﾟ)", richText.plainText)
        assertEquals(1, richText.segments.size)
        assertFalse(richText.segments[0].isBold)
        assertFalse(richText.segments[0].isSmall)
        assertNull(richText.segments[0].linkTarget)
    }

    @Test
    fun `rich text render hint only marks actual rich markup`() {
        assertFalse(shouldRenderNmbRichText("普通正文"))
        assertTrue(shouldRenderNmbRichText("<font color=\"#789922\">引用</font>"))
        assertTrue(shouldRenderNmbRichText("[h]隐藏内容[/h]"))
    }

    @Test
    fun `collapses consecutive html spaces`() {
        val richText = parseNmbRichText("前文   <b> 中间 </b>   后文")

        assertEquals("前文 中间 后文", richText.plainText)
    }

    @Test
    fun `collapses tab characters like html whitespace`() {
        val richText = parseNmbRichText("前文\t\t<b>\t中间\t</b>\t后文")

        assertEquals("前文 中间 后文", richText.plainText)
    }
}
